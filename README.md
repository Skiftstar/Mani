This is a Kotlin Multiplatform project targeting Android, Desktop (JVM).

* [/shared](./shared/src) contains the code shared across the Android and desktop
  apps - UI (Compose Multiplatform) and business logic alike. It contains several
  subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that's common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated
    in the folder name. For example, the [jvmMain](./shared/src/jvmMain/kotlin) folder is
    for the Desktop (JVM) specific part, and [androidMain](./shared/src/androidMain/kotlin)
    for the Android-specific part.
* [/androidApp](./androidApp/src) is the thin entry-point module that builds into the
  Android application (`MainActivity`, manifest, launcher icons) - it depends on `shared`.
* [/desktopApp](./desktopApp/src) is the thin entry-point module that builds into the
  desktop (JVM) application (`main()`, packaging config) - it depends on `shared`.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :androidApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :androidApp:assembleDebug
  ```

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :desktopApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :desktopApp:run
  ```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
