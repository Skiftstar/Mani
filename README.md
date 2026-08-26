# Mani

A Subsonic/Navidrome music client for desktop and Android, built with Kotlin
Multiplatform and Compose Multiplatform. Desktop playback is driven by
[mpv](https://mpv.io/) over its JSON IPC socket, with MPRIS integration on
Linux (so your desktop's own media controls/OSD, shell widgets, etc. all work
against it); Android playback runs on Media3/ExoPlayer as a background
service, with the same lock-screen/notification controls any other Android
music app gets. Both share a Spotify-style set of features: search,
playlists, Liked Songs, scrobbling.

## Installation

Prebuilt packages are attached to each [GitHub Release](../../releases):

| Platform | Format |
| --- | --- |
| Windows | `.msi` installer |
| Linux (Debian/Ubuntu and derivatives) | `.deb` |
| Linux (any distro) | `.AppImage` |
| Arch Linux (AUR) | TODO - not published yet |
| Android | TODO - not published yet (build a debug APK from source, see below) |

Linux `.deb` installs declare `mpv` as a package dependency, so your package
manager installs it automatically alongside Mani if it isn't already
present. The Windows `.msi` bundles its own copy of `mpv` directly, since
Windows has no equivalent system package manager to depend on it through.

## Building from source

**Prerequisites** (all platforms): JDK 17 or newer. For Android specifically,
an installed Android SDK (the easiest way is via
[Android Studio](https://developer.android.com/studio)) with `platform-tools`
on `PATH` for `adb`.

### Run the development build

- Linux (desktop): `./gradlew :desktopApp:run`
- Windows (desktop): `.\gradlew.bat :desktopApp:run`
- Android: `./gradlew :androidApp:installDebug` (or `.\gradlew.bat` on
  Windows) to install onto a connected device or a running emulator, then
  launch **Mani** from the app drawer - or skip the app drawer and run
  `adb shell am start -n xyz.skifty.mani/.MainActivity` directly.

Desktop's dev build runs directly against your system's installed `mpv` (on
PATH) - no packaging step involved, the quickest way to iterate.

### Package a distributable build

| Target | Command | Extra build-machine prerequisites |
| --- | --- | --- |
| Windows `.msi` | `.\gradlew.bat :desktopApp:packageReleaseMsi` | [7-Zip](https://www.7-zip.org/) (`7z` on PATH) - used to fetch and unpack the bundled `mpv` build |
| Linux `.deb` | `./gradlew :desktopApp:packageReleaseDeb :desktopApp:addMpvDependencyToDeb` | `dpkg-deb`/`fakeroot` (e.g. `sudo apt install dpkg-dev fakeroot`, or `sudo pacman -S dpkg fakeroot` on non-Debian distros) |
| Linux `.AppImage`-style app-image | `./gradlew :desktopApp:packageReleaseAppImage` | - |
| Android (debug) | `./gradlew :androidApp:assembleDebug` | - |

`addMpvDependencyToDeb` must run *after* `packageReleaseDeb` - it patches the
already-built `.deb`'s dependency list, since Compose's Gradle plugin
doesn't expose a way to declare that directly.

### Arch Linux, via the local `PKGBUILD`

[`packaging/arch/PKGBUILD`](packaging/arch/PKGBUILD) builds and installs
Mani the normal Arch way:

```shell
cd packaging/arch
makepkg -si
```

This isn't published to the AUR yet (see the Installation table above) - it
clones from this repo's GitHub remote, so push any local changes you want
reflected before running it.

## Project structure

This is a Kotlin Multiplatform project targeting Android and Desktop (JVM):

* [`/shared`](./shared/src) contains the code shared across the Android and
  desktop apps - UI (Compose Multiplatform) and business logic alike.
  - [`commonMain`](./shared/src/commonMain/kotlin) is for code common to all
    targets - most screens/components, the Subsonic API client, and the
    `AudioPlayer` interface both platforms implement against.
  - Other folders are for Kotlin code compiled only for the platform named
    in the folder - [`jvmMain`](./shared/src/jvmMain/kotlin) for the desktop
    (JVM) target (window chrome, the sidebar, mpv/MPRIS integration,
    Windows/Linux secure storage), [`androidMain`](./shared/src/androidMain/kotlin)
    for Android (the bottom nav shell and Now Playing screen, the
    Media3/ExoPlayer-backed playback service, Android Keystore-backed secure
    storage).
  - Dependency injection is [Koin](https://insert-koin.io/) - a common module
    for what's shared, plus one module per platform for what isn't (see
    `di/` under each source set above).
* [`/androidApp`](./androidApp/src) is the thin entry-point module that
  builds the Android application (`MainActivity`, `ManiApplication`,
  manifest, launcher icons) - depends on `shared`.
* [`/desktopApp`](./desktopApp/src) is the thin entry-point module that
  builds the desktop (JVM) application (`main()`, packaging config) -
  depends on `shared`.
* [`/packaging`](./packaging) holds distro-specific packaging files not
  produced directly by Gradle (currently just the Arch `PKGBUILD`).

## Roadmap

- [ ] Profile settings (both platforms)
- [ ] Home screen layout (both platforms - currently an empty stub on each)
- [ ] Playlist creation
- [x] Subsonic/Navidrome login & session persistence
- [x] Playback, with seeking, volume, and a progress bar (mpv-backed on
      desktop; Media3/ExoPlayer-backed on Android, as a foreground service
      with lock-screen/notification controls)
- [x] MPRIS integration (Linux desktop media controls/OSD)
- [x] Playback queue (shuffle, loop, skip)
- [x] Search
- [x] Liked Songs
- [x] Playlist browsing, including each playlist's total song count/runtime
- [x] Right-click song context menu (desktop only - play, add to queue,
      like/unlike, add to/remove from playlist)
- [x] Scrobbling
- [x] Global Space-to-pause keybind (desktop only)
- [x] Windows/Linux packaging (`.msi`, `.deb`, `.AppImage`, Arch `PKGBUILD`)
- [x] Android navigation shell - bottom nav (Home/Search/Liked Songs/
      Playlists/Profile), a mini-player above it whenever something's
      playing, and a full-screen Now Playing view that expands from it with
      an animated transition (swipe up from there to jump to the playing
      song's playlist, or back-gesture to collapse)
