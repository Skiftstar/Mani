# Third-party licenses

Moonlight itself is MIT-licensed (see [LICENSE](LICENSE)).

## mpv

Moonlight drives [mpv](https://mpv.io/) as a separate subprocess over its
JSON IPC socket for audio playback - it's never linked into Moonlight's own
process, only spawned and talked to over that IPC connection. On Linux, mpv
is a declared package dependency (installed separately via your package
manager, e.g. `apt install mpv` / `pacman -S mpv`) rather than something
Moonlight redistributes itself, so nothing further is needed there. On
Windows, where there's no equivalent system package manager to depend on
mpv through, Moonlight bundles mpv's own binary directly (see
`desktopApp/resources/windows/`).

mpv is licensed under the GPL, version 2 or later, by default - a full copy
of that license is included alongside the bundled binary at
[desktopApp/resources/windows/mpv-LICENSE.txt](desktopApp/resources/windows/mpv-LICENSE.txt)
(fetched verbatim from mpv's own repository, not paraphrased). Because mpv
is only spawned as a subprocess rather than linked into Moonlight, this is
"mere aggregation" under the GPL - it doesn't require Moonlight's own source
to be GPL-licensed - but redistributing the binary itself still carries the
GPL's usual obligation to include its license text, which is what this file
and the one it points to are for.

The bundled Windows build comes from
[shinchiro/mpv-winbuild-cmake](https://github.com/shinchiro/mpv-winbuild-cmake)'s
releases. 