package xyz.skifty.mani.di

import org.koin.dsl.module
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo

/** Bindings shared by every platform. [xyz.skifty.mani.media.AudioPlayer],
 *  [xyz.skifty.mani.preferences.AppPreferences], and [xyz.skifty.mani.security.SecureStorage] are
 *  deliberately not bound here - their concrete implementations are platform-specific, bound in
 *  each platform's own module (see DesktopModule/AndroidModule). */
val commonModule = module {
    single { ApiService() }
    single { SongInfo() }
    single {
        PlaybackQueue(
            audioPlayer = get(),
            activeSongInfo = get(),
        )
    }
    single { PlaylistLibrary() }
}
