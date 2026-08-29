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
            implementation(libs.koin.core)
        }
    }
}

// Pulled out to top-level vals so packageReleaseAppImageBundle below can reuse the same values
// rather than re-hardcoding them.
//
// jpackage's --name (Compose's nativeDistributions.packageName) is a single value shared by every
// target format in this one nativeDistributions block - confirmed by decompiling this project's
// resolved Compose Gradle plugin jar (org.jetbrains.compose:compose-gradle-plugin:1.12.0):
// WindowsPlatformSettings carries no packageName field of its own (unlike LinuxPlatformSettings and
// AbstractMacOSPlatformSettings, which do), and AbstractJPackageTask feeds jpackage's --name flag
// from this one shared property in every case. The only Linux-specific override that exists,
// linux.packageName, feeds a *different* jpackage flag, --linux-package-name, which only renames the
// built .deb's "Package:" control-file metadata - not the actual installed exec name, app-image
// folder, or .desktop Exec=/Icon= values jpackage derives from --name, which
// packageReleaseAppImageBundle below still relies on being "xyz.skifty.mani". So there's no DSL
// property that overrides just the Windows-facing name while leaving Linux's untouched.
//
// What makes swapping this one shared value safe anyway: this build only ever registers one
// jpackage-backed task per host OS - Compose's plugin only registers packageMsi/packageReleaseMsi on
// Windows hosts, and packageDeb/packageAppImage on Linux hosts (see the packageMsi tasks.matching
// comment further down) - so a Windows host building packageReleaseMsi never also needs the Linux
// name in the same run, and vice versa. TargetFormat.Msi.isCompatibleWithCurrentOS() - the same check
// Compose's own plugin uses to decide whether to register packageMsi at all - picks "Mani" (a clean
// Windows installer/Start-Menu/Add-Remove-Programs/install-dir name) on Windows hosts, and leaves the
// existing "xyz.skifty.mani" value in place everywhere else, so the .deb/.AppImage naming this file's
// Linux-only tasks below depend on is unaffected.
val desktopPackageName = if (TargetFormat.Msi.isCompatibleWithCurrentOS) {
    "Mani"
} else {
    "xyz.skifty.mani"
}
val desktopPackageVersion = "1.0.0"

compose.desktop {
    application {
        mainClass = "xyz.skifty.mani.MainKt"

        // Silences the JDK's "restricted method in java.lang.System has been called" warning
        // logged on every run - Skiko loads its native library via System.load, which newer JDKs
        // flag unless native access is explicitly granted to the unnamed module it runs in.
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")

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
            packageName = desktopPackageName
            packageVersion = desktopPackageVersion
            vendor = "Mani"

            // Windows only ("windows {}" is a no-op on other hosts - jpackage's --win-* flags
            // simply don't apply there). Setting menuGroup here doesn't just group the Start Menu
            // entry - it's also what turns the entry on in the first place: Compose's own
            // WindowsPlatformSettings.getMenu() (confirmed by decompiling WindowsPlatformSettings)
            // is a computed `menu || menuGroup != null`, and neither was ever set before this
            // block existed, so jpackage's --win-menu flag was never passed and no Start Menu
            // shortcut was created at all. upgradeUuid is pinned rather than left for jpackage to
            // auto-derive from packageName, since that derivation would otherwise silently change
            // the MSI's UpgradeCode the moment packageName above changed from "xyz.skifty.mani" to
            // "Mani", breaking upgrade-in-place detection against any previously published release.
            windows {
                menuGroup = "Mani"
                upgradeUuid = "25c62f15-4c9a-4f5f-9190-08d6cc8f8972"
                // Referenced straight from the top-level icons/ folder (this project's one
                // canonical source for icon art) rather than a copy kept under desktopApp/ -
                // nothing to drift out of sync. jpackage's Windows --icon flag needs an actual
                // .ico, not a raster image.
                iconFile.set(rootProject.file("icons/mani.ico"))
            }

            // No macOS support - see targetFormats above still listing Dmg for why that's not
            // removed outright, but there's deliberately no macOS { iconFile.set(...) } block
            // here either.
            linux {
                // The opaque tile art (icon + its own background), not the transparent glyph -
                // Linux's iconFile wants a flat raster with no transparency to look right in a
                // .desktop launcher/taskbar. Feeds jpackage's auto-generated lib/xyz.skifty.mani.png,
                // which packaging/arch/mani.desktop's Icon= path and packageReleaseAppImageBundle
                // below both already consume by that fixed jpackage naming convention - neither
                // needs to change.
                iconFile.set(rootProject.file("icons/png/mani-tile-512.png"))
            }

            // The packaged app ships a custom jlink-trimmed JDK runtime (createRuntimeImage).
            // includeAllModules=true was tried first (full JDK, ~150MB, no jdeps guesswork) after
            // jdk.security.auth (com.sun.security.auth.module.UnixSystem) turned up missing from
            // plain automatic detection - dbus-java's SASL authentication only reaches it
            // reflectively, breaking MPRIS/the Linux keyring integration silently in a packaged
            // build despite working fine under a plain `./gradlew run` (full JDK, every module
            // present). Going back to an explicit list instead (~70MB automatic baseline + that
            // one confirmed reflective gap) rather than the full set - if another reflective gap
            // like this surfaces later (jna/dbus-java/slf4j are all candidates, per the identical
            // reflection pattern noted in the ProGuard comment above), add the specific missing
            // module here by hand rather than reaching back for includeAllModules=true.
            modules(
                // Automatic detection's own result (confirmed by hand: this exact set is what
                // jdeps-based analysis produces with includeAllModules left off) - kept explicit
                // rather than just omitting `modules(...)` entirely, so this list stays the
                // single source of truth for every module this app needs, confirmed ones and
                // automatically-detected ones alike.
                "java.base",
                "java.desktop",
                "java.logging",
                "jdk.crypto.ec",
                // Confirmed-by-hand reflective gap - see the comment above.
                "jdk.security.auth",
            )

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

// Deliberately not using appimagetool (the "official" packaging CLI) here - it's distributed as an
// AppImage itself, and both its FUSE-mount path and its APPIMAGE_EXTRACT_AND_RUN fallback were
// confirmed by hand to crash outright ("fuse: memory allocation failed" / "Can't open squashfs
// image: Bad address") against the bundled squashfuse's handling of newer kernels - a known upstream
// squashfuse/libfuse incompatibility, unrelated to this project. Building the .AppImage by hand
// instead - mksquashfs the AppDir, concatenate a plain (non-AppImage-wrapped) runtime binary in
// front of it - sidesteps needing to execute any AppImage, and thus that bug, entirely at build
// time. type2-runtime publishes exactly that raw runtime binary; unlike appimagetool's own
// "continuous"-only releases, it also tags dated releases, so this pins to one of those for
// reproducible builds the same way downloadMpvForWindows above pins mpv-winbuild-cmake - bump
// appImageRuntimeTag by hand when a newer runtime build is wanted.
val downloadAppImageRuntime = tasks.register("downloadAppImageRuntime") {
    val runtimeFile = layout.buildDirectory.file("appimageRuntime/runtime-x86_64")

    doLast {
        val appImageRuntimeTag = "20251108"

        val file = runtimeFile.get().asFile
        if (file.exists()) {
            return@doLast // already fetched
        }

        file.parentFile.mkdirs()
        URI("https://github.com/AppImage/type2-runtime/releases/download/$appImageRuntimeTag/runtime-x86_64")
            .toURL()
            .openStream()
            .use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        file.setExecutable(true)
    }
}

// Compose's own `AppImage` target format (used by packageReleaseAppImage above) is just an alias
// for jpackage's native `app-image` type on Linux - a portable bin/+lib/ folder, not an actual
// single-file .AppImage (confirmed by hand: no AppRun, no root .desktop file, no squashfs bundle
// anywhere in its output). This task wraps that folder into a real .AppImage by hand, the same way
// addMpvDependencyToDeb above patches jpackage's .deb output rather than getting Compose's plugin
// to do it directly.
//
// Requires `mksquashfs` on PATH (Arch: `sudo pacman -S squashfs-tools`; Debian/Ubuntu: `sudo apt
// install squashfs-tools`) - a build-time dependency in the same vein as dpkg-deb/7z assumed
// present elsewhere in this file, just less universally preinstalled.
//
// Like the .deb, this doesn't bundle mpv - Linux relies on a system mpv package either way (see
// addMpvDependencyToDeb's comment above), so running the resulting AppImage still requires mpv
// installed separately.
tasks.register("packageReleaseAppImageBundle") {
    dependsOn("packageReleaseAppImage", downloadAppImageRuntime)

    // Captured here as lazy Providers/values for the same configuration-cache reason as
    // addMpvDependencyToDeb's debOutputDir/patchDir above - this also applies to plain top-level
    // vals like desktopPackageName/desktopPackageVersion, not just functions, since referencing
    // them directly from doLast would likewise capture a live reference to this script instance.
    val appImageDir = layout.buildDirectory.dir("compose/binaries/main-release/app/$desktopPackageName")
    val appDirStaging = layout.buildDirectory.dir("appImageAppDir")
    val outputDir = layout.buildDirectory.dir("compose/binaries/main-release/appimage")
    val runtimeFile = layout.buildDirectory.file("appimageRuntime/runtime-x86_64")
    val packageName = desktopPackageName
    val packageVersion = desktopPackageVersion

    doLast {
        // Local to this action, not a script-top-level function - see addMpvDependencyToDeb's
        // runCommand above for why that distinction matters for the configuration cache.
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

        val appDir = appDirStaging.get().asFile
        appDir.deleteRecursively()
        appDir.mkdirs()

        appImageDir.get().asFile.copyRecursively(
            target = appDir,
            overwrite = true,
        )

        // AppRun is AppImage's fixed entry point name - it's what the resulting .AppImage actually
        // executes, relative to wherever it happens to extract/mount itself, hence resolving `bin/`
        // relative to AppRun's own location rather than a fixed absolute path.
        val appRunFile = appDir.resolve("AppRun")
        appRunFile.writeText(
            """
            #!/bin/sh
            HERE="${'$'}(dirname "${'$'}(readlink -f "${'$'}0")")"
            exec "${'$'}HERE/bin/$packageName" "${'$'}@"
            """.trimIndent(),
        )
        appRunFile.setExecutable(true)

        // Wording matches packaging/arch/mani.desktop's .desktop entry for consistency across
        // package formats - jpackage's own auto-generated .desktop (baked into the .deb) settles
        // for the bare package name and Categories=Unknown instead.
        appDir.resolve("$packageName.desktop")
            .writeText(
                """
                [Desktop Entry]
                Type=Application
                Name=Mani
                Comment=A Subsonic/Navidrome desktop music client
                Exec=$packageName
                Icon=$packageName
                Terminal=false
                Categories=AudioVideo;Audio;Player;
                """.trimIndent(),
            )

        // AppImage expects its icon at the AppDir root, unlike jpackage's own app-image layout
        // which only carries it inside lib/ - reusing that already-generated file rather than
        // needing a separate source icon of our own.
        appDir.resolve("lib/$packageName.png")
            .copyTo(
                target = appDir.resolve("$packageName.png"),
                overwrite = true,
            )

        val outputDirFile = outputDir.get().asFile
        outputDirFile.mkdirs()
        val outputFile = outputDirFile.resolve("$packageName-$packageVersion-x86_64.AppImage")
        outputFile.delete()

        val squashfsFile = appDirStaging.get().asFile.parentFile.resolve("appImage.squashfs")
        squashfsFile.delete()
        runCommand(
            "mksquashfs",
            appDir.absolutePath,
            squashfsFile.absolutePath,
            "-root-owned",
            "-noappend",
        )

        // A .AppImage is just a runtime binary with a squashfs filesystem appended directly after
        // it - the runtime locates that appended data by reading its own ELF layout at startup, so
        // plain concatenation (no header patching) is all that's needed here.
        outputFile.outputStream().use { output ->
            runtimeFile.get().asFile.inputStream().use { input -> input.copyTo(output) }
            squashfsFile.inputStream().use { input -> input.copyTo(output) }
        }
        outputFile.setExecutable(true)
        squashfsFile.delete()

        println("The AppImage is written to ${outputFile.absolutePath}")
    }
}
