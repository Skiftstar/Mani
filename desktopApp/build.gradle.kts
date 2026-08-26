import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(projects.shared)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "xyz.skifty.mani.MainKt"

        // ProGuard shrinking broke real Windows/Linux installs at runtime - confirmed by hand:
        // ktor-client-cio's engine is only ever loaded reflectively (via ServiceLoader, reading a
        // META-INF/services entry naming the class - nothing in this codebase references
        // CIOEngineContainer directly), so ProGuard's static analysis doesn't see it's needed and
        // strips it, leaving the service file pointing at a class that's gone
        // (ServiceConfigurationError at startup). This is a known, still-unresolved upstream gap -
        // Ktor doesn't ship JVM-target ProGuard rules yet (KTOR-6703/KTOR-7056). A narrow -keep
        // rule for just the CIO engine wouldn't be enough either: the same build log already shows
        // the identical "accesses ... dynamically" reflection pattern for jna/dbus-java/slf4j too
        // (MPRIS, Windows DPAPI) - undetected until whichever of those code paths actually runs.
        // Disabling shrinking entirely trades some install size for not chasing this class of bug
        // one library at a time.
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "xyz.skifty.mani"
            packageVersion = "1.0.0"

            // The packaged app ships a custom jlink-trimmed JDK runtime (createRuntimeImage),
            // built from whatever modules static analysis detects the app needs - confirmed by
            // hand to miss jdk.security.auth (com.sun.security.auth.module.UnixSystem), which
            // dbus-java's SASL authentication only reaches reflectively, breaking MPRIS silently
            // in the packaged build despite working fine under a plain `./gradlew run` (full JDK,
            // every module present). Same reasoning as disabling ProGuard above: a narrow
            // `modules("jdk.security.auth")` fix would only address today's specific symptom, not
            // whatever else jna/dbus-java might reach reflectively elsewhere and hasn't surfaced
            // yet - full JDK modules trades runtime size for not chasing this class of bug one
            // missing module at a time.
            includeAllModules = true

            // Bundled per-OS extra files - only resources/windows/ is populated (mpv.exe +
            // d3dcompiler_43.dll, fetched at build time by downloadMpvForWindows below rather than
            // committed - see that task for why). resources/linux/ and resources/macos/ are the
            // equivalent special-cased folder names Compose recognizes, left absent since Linux
            // depends on a system mpv package instead (see addMpvDependencyToDeb below) and macOS
            // isn't a target.
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
        }
    }
}

// Compose's Gradle plugin doesn't expose jpackage's --linux-package-deps (confirmed by
// inspecting LinuxPlatformSettings' actual bytecode - no such field exists, despite what a web
// search suggested), so this reproduces what that flag would do by hand: unpack the .deb jpackage
// already built, add an mpv dependency to its control file, repack over the original. Registered
// as a plain task (not an Exec task) since this needs multiple sequential steps - extract, edit a
// file with plain Kotlin, repack - not a single command line.
//
// mpv (>= 0.38.0): DesktopAudioPlayer.prepare() relies on loadfile's per-file-options syntax
// (the third positional index argument must be -1 for the fourth to be honored), only correct
// since that mpv version - see that function's own comment.
tasks.register("addMpvDependencyToDeb") {
    dependsOn("packageReleaseDeb")

    // Captured here, at configuration time, as lazy Providers rather than resolved inside doLast -
    // the task action below must not hold a live reference to `project`/`layout` themselves
    // (needed to resolve these), which the configuration cache disallows serializing. Only calling
    // .get() on these already-captured Providers inside doLast keeps it cache-compatible.
    val debOutputDir = layout.buildDirectory.dir("compose/binaries/main-release/deb")
    val patchDir = layout.buildDirectory.dir("mpvDebPatch")

    doLast {
        // Local to this action, not a script-top-level function - every top-level declaration in
        // a .gradle.kts file is compiled as a member of one synthesized class representing the
        // whole script, so calling it from here would capture a reference to that script instance
        // to dispatch the call (regardless of whether the function itself touches any script
        // state), which the configuration cache doesn't allow serializing. Confirmed by hand: this
        // was exactly the second (and, once fixed, last) of the two config-cache errors this task
        // originally hit.
        fun runCommand(vararg command: String) {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw GradleException("${command.joinToString(" ")} failed (exit $exitCode):\n$output")
            }
        }

        val debFile = debOutputDir.get()
            .asFileTree
            .matching { include("*.deb") }
            .singleFile

        val extractDir = patchDir.get().asFile
        extractDir.deleteRecursively()
        extractDir.mkdirs()

        runCommand("dpkg-deb", "-R", debFile.absolutePath, extractDir.absolutePath)

        val controlFile = extractDir.resolve("DEBIAN/control")
        val originalText = controlFile.readText()
        val dependsLinePattern = Regex("^Depends:(.*)$", RegexOption.MULTILINE)
        val existingDependsLine = dependsLinePattern.find(originalText)
        // Package names are the first token of each comma-separated entry, before any
        // "(>= version)" constraint - this task has no declared outputs for Gradle to use for its
        // own up-to-date check (it mutates packageReleaseDeb's output file in place rather than
        // producing a distinct one), so it re-runs on every explicit invocation - this guard is
        // what keeps re-running it harmless instead of appending "mpv" again each time.
        val alreadyHasMpv = existingDependsLine?.groupValues?.get(1)
            ?.split(",")
            ?.any { entry -> entry.trim().substringBefore(' ') == "mpv" }
            ?: false
        val patchedText = when {
            alreadyHasMpv -> originalText
            existingDependsLine != null -> originalText.replace(dependsLinePattern) { match ->
                "Depends:${match.groupValues[1].trimEnd()}, mpv (>= 0.38.0)"
            }
            else -> originalText.trimEnd() + "\nDepends: mpv (>= 0.38.0)\n"
        }
        if (patchedText != originalText) {
            controlFile.writeText(patchedText)
            runCommand("dpkg-deb", "-b", extractDir.absolutePath, debFile.absolutePath)
        }
    }
}

// mpv.exe is ~117MB once built with this project's feature set - too large for a normal git push
// (GitHub rejects any single file over 100MB), so it's fetched here at build time instead of
// committed, from a pinned github.com/shinchiro/mpv-winbuild-cmake release (the same project
// MpvIpcClient's own missing-mpv install instructions already point Windows users to). Pinned by
// release tag rather than "latest", for reproducible builds - bump mpvWindowsBuildTag by hand
// when a newer build is wanted. Each release's asset filenames carry a build-specific git-commit
// suffix that isn't predictable ahead of time (e.g. "mpv-x86_64-20260814-git-7b8915bc1d.7z"), so
// the exact filename is resolved via the GitHub API rather than guessed or hardcoded - confirmed
// by hand against the real API response, matching only the plain x86_64 build (excluding the
// "-dev-" headers/import-lib package and the "-v3-" variant, which needs a newer CPU baseline).
val downloadMpvForWindows = tasks.register("downloadMpvForWindows") {
    val windowsResourcesDir = layout.projectDirectory.dir("resources/windows")
    val archiveDownloadFile = layout.buildDirectory.file("mpvWindowsDownload/mpv.7z")

    doLast {
        // Local to this action, not a script-top-level val/function - see addMpvDependencyToDeb's
        // runCommand above for why that distinction matters for the configuration cache.
        val mpvWindowsBuildTag = "20260814"

        fun runCommand(vararg command: String) {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw GradleException("${command.joinToString(" ")} failed (exit $exitCode):\n$output")
            }
        }

        val exeFile = windowsResourcesDir.file("mpv.exe").asFile
        val dllFile = windowsResourcesDir.file("d3dcompiler_43.dll").asFile
        if (exeFile.exists() && dllFile.exists()) {
            return@doLast // already fetched - avoid re-downloading a >100MB archive every build
        }

        val releaseJson = URI(
            "https://api.github.com/repos/shinchiro/mpv-winbuild-cmake/releases/tags/$mpvWindowsBuildTag",
        )
            .toURL()
            .readText()
        val assetFileName = Regex(""""name":\s*"(mpv-x86_64-\d{8}-git-[0-9a-f]+\.7z)"""")
            .find(releaseJson)
            ?.groupValues
            ?.get(1)
            ?: throw GradleException(
                "Could not find an mpv-x86_64-<date>-git-<hash>.7z asset in release " +
                    "$mpvWindowsBuildTag - check " +
                    "https://github.com/shinchiro/mpv-winbuild-cmake/releases/tag/$mpvWindowsBuildTag " +
                    "by hand and update mpvWindowsBuildTag if needed.",
            )
        // GitHub's release-asset download URLs follow this fixed pattern regardless of the
        // asset's real (signed, expiring) storage location - confirmed by hand rather than parsed
        // out of the API response's browser_download_url field, which needs pairing back up with
        // the matched "name" field above; this sidesteps that entirely.
        val assetUrl = "https://github.com/shinchiro/mpv-winbuild-cmake/releases/download/" +
            "$mpvWindowsBuildTag/$assetFileName"

        val archiveFile = archiveDownloadFile.get().asFile
        archiveFile.parentFile.mkdirs()
        URI(assetUrl).toURL().openStream().use { input ->
            archiveFile.outputStream().use { output -> input.copyTo(output) }
        }

        // Extracted via the system `7z` CLI, not a JVM library (commons-compress + its optional
        // xz dependency were tried first, but the extra xz.jar this project's buildscript{} block
        // declared for 7z's LZMA2 filter genuinely wasn't reachable from this task's execution
        // classloader at runtime - confirmed by hand, NoClassDefFoundError on org.tukaani.xz
        // classes despite the jar being correctly resolved into the Gradle cache - a Gradle
        // subproject-buildscript-classpath limitation, not something worth continuing to fight).
        // `7z e` (not `x`) extracts matched files flat, ignoring the archive's own internal
        // directory structure - exactly the two files named here, nothing else, straight into
        // resources/windows/ with no subfolder - confirmed by hand against the real archive.
        windowsResourcesDir.asFile.mkdirs()
        runCommand(
            "7z", "e", archiveFile.absolutePath,
            "-o${windowsResourcesDir.asFile.absolutePath}",
            "mpv.exe", "d3dcompiler_43.dll",
            "-r", "-y",
        )
        archiveFile.delete()

        if (!exeFile.exists() || !dllFile.exists()) {
            throw GradleException(
                "mpv.exe/d3dcompiler_43.dll weren't found inside the downloaded archive - its " +
                    "internal layout may have changed; check $assetUrl by hand.",
            )
        }
    }
}

// tasks.named("packageMsi") would throw immediately if that task doesn't exist, rather than being
// a harmless no-op - and on a non-Windows host, it genuinely doesn't: Compose's plugin only
// registers Msi-producing tasks at all when TargetFormat.Msi is compatible with the current OS
// (confirmed by hand - this failed configuring on this Linux machine before switching to
// tasks.matching, unlike packageDeb/packageReleaseDeb, which stay registered on Linux even though
// the underlying jpackage invocation needs Debian tooling only present at *execution* time).
// tasks.matching{}.configureEach{} is lazy-safe either way, so this wiring is a no-op on Linux/macOS
// and takes effect only where a task actually named this exists.
tasks.matching { task -> task.name == "packageMsi" || task.name == "packageReleaseMsi" }
    .configureEach { dependsOn(downloadMpvForWindows) }
