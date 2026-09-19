# Third-party licenses

Mani itself is GPLv3-licensed (see [LICENSE](LICENSE)).

## mpv

Mani drives [mpv](https://mpv.io/) for audio playback, differently per platform:

- **Linux**: mpv runs as a separate subprocess, talked to over its JSON IPC
  socket - never linked into Mani's own process. mpv is a declared package
  dependency there (installed separately via your package manager, e.g.
  `apt install mpv` / `pacman -S mpv`) rather than something Mani
  redistributes itself.
- **Windows**: Mani links directly against `libmpv-2.dll` (mpv's own C
  embedding API) in-process, via JNA - no subprocess, no IPC. The DLL is
  bundled since there's no equivalent system package manager to depend on
  mpv through on Windows (see `desktopApp/resources/windows/`).

mpv is licensed under the GPL, version 2 or later, by default - a full copy
of that license is included alongside the bundled Windows library at
[desktopApp/resources/windows/mpv-LICENSE.txt](desktopApp/resources/windows/mpv-LICENSE.txt)
(fetched verbatim from mpv's own repository, not paraphrased). Mani being
GPLv3-licensed itself is what makes the Windows build's in-process linking
straightforward under the GPL - combining GPL-licensed code into a single
program is exactly what the GPL is designed to permit, without needing to
reason about "mere aggregation" the way a fully separate, MIT-licensed
process talking over IPC would have to (still the case on Linux, where
nothing is actually linked together at all).

The bundled Windows library comes from
[shinchiro/mpv-winbuild-cmake](https://github.com/shinchiro/mpv-winbuild-cmake)'s
releases.
