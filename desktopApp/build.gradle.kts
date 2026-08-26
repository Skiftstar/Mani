import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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
        mainClass = "xyz.skifty.moonlight.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "xyz.skifty.moonlight"
            packageVersion = "1.0.0"

            // Bundled per-OS extra files - only resources/windows/ is populated today (mpv.exe +
            // its DLLs, since Windows has no system package manager to depend on mpv through
            // instead - see MpvIpcClient.resolveMpvExecutable()). resources/linux/ and
            // resources/macos/ are the equivalent special-cased folder names Compose recognizes,
            // left absent since Linux depends on a system mpv package instead (see
            // addMpvDependencyToDeb below) and macOS isn't a target.
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
