# Moonlight

A Subsonic/Navidrome desktop music client, built with Kotlin Multiplatform
and Compose Multiplatform. Playback is driven by [mpv](https://mpv.io/) over
its JSON IPC socket, with MPRIS integration on Linux (so your desktop's own
media controls/OSD, shell widgets, etc. all work against it) and a 
Spotify-style set of features on top: search, playlists, Liked Songs,
scrobbling.

## Installation

Prebuilt packages are attached to each [GitHub Release](../../releases):

| Platform | Format |
| --- | --- |
| Windows | `.msi` installer |
| Linux (Debian/Ubuntu and derivatives) | `.deb` |
| Linux (any distro) | `.AppImage` |
| Arch Linux (AUR) | TODO - not published yet |

Linux `.deb` installs declare `mpv` as a package dependency, so your package
manager installs it automatically alongside Moonlight if it isn't already
present. The Windows `.msi` bundles its own copy of `mpv` directly, since
Windows has no equivalent system package manager to depend on it through.

## Building from source

**Prerequisites** (all platforms): JDK 17 or newer.

### Run the development build

- Linux: `./gradlew :desktopApp:run`
- Windows: `.\gradlew.bat :desktopApp:run`

This runs directly against your system's installed `mpv` (on PATH) - no
packaging step involved, the quickest way to iterate.

### Package a distributable build

| Target | Command | Extra build-machine prerequisites |
| --- | --- | --- |
| Windows `.msi` | `.\gradlew.bat :desktopApp:packageReleaseMsi` | [7-Zip](https://www.7-zip.org/) (`7z` on PATH) - used to fetch and unpack the bundled `mpv` build; the [WiX Toolset](https://wixtoolset.org/), which `jpackage` itself needs to produce an `.msi` |
| Linux `.deb` | `./gradlew :desktopApp:packageReleaseDeb :desktopApp:addMpvDependencyToDeb` | `dpkg-deb`/`fakeroot` (e.g. `sudo apt install dpkg-dev fakeroot`, or `sudo pacman -S dpkg fakeroot` on non-Debian distros) |
| Linux `.AppImage`-style app-image | `./gradlew :desktopApp:packageReleaseAppImage` | - |
| Android (debug) | `./gradlew :androidApp:assembleDebug` | - |

`addMpvDependencyToDeb` must run *after* `packageReleaseDeb` - it patches the
already-built `.deb`'s dependency list, since Compose's Gradle plugin
doesn't expose a way to declare that directly.

### Arch Linux, via the local `PKGBUILD`

[`packaging/arch/PKGBUILD`](packaging/arch/PKGBUILD) builds and installs
Moonlight the normal Arch way:

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
    targets.
  - Other folders are for Kotlin code compiled only for the platform named
    in the folder - [`jvmMain`](./shared/src/jvmMain/kotlin) for the desktop
    (JVM) target, [`androidMain`](./shared/src/androidMain/kotlin) for
    Android.
* [`/androidApp`](./androidApp/src) is the thin entry-point module that
  builds the Android application (`MainActivity`, manifest, launcher icons)
  - depends on `shared`.
* [`/desktopApp`](./desktopApp/src) is the thin entry-point module that
  builds the desktop (JVM) application (`main()`, packaging config) -
  depends on `shared`.
* [`/packaging`](./packaging) holds distro-specific packaging files not
  produced directly by Gradle (currently just the Arch `PKGBUILD`).

## Roadmap

- [ ] Profile settings
- [ ] Home screen layout
- [ ] Playlist creation
- [ ] Android layout (whole thing)
- [x] Subsonic/Navidrome login & session persistence
- [x] Playback (mpv-backed, with seeking, volume, and a progress bar)
- [x] MPRIS integration (Linux desktop media controls/OSD)
- [x] Playback queue (shuffle, loop, skip)
- [x] Search
- [x] Liked Songs
- [x] Playlist browsing
- [x] Right-click song context menu (play, add to queue, like/unlike, add to/remove from playlist)
- [x] Scrobbling
- [x] Global Space-to-pause keybind
- [x] Windows/Linux packaging (`.msi`, `.deb`, `.AppImage`, Arch `PKGBUILD`)
