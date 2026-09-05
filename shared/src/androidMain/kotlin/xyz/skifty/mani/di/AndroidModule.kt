package xyz.skifty.mani.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import xyz.skifty.mani.media.AndroidAudioPlayer
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.VisualizerState
import xyz.skifty.mani.preferences.AndroidAppPreferences
import xyz.skifty.mani.preferences.AppPreferences
import xyz.skifty.mani.security.AndroidSecureStorage
import xyz.skifty.mani.security.SecureStorage

val androidModule = module {
    single<AudioPlayer> { AndroidAudioPlayer(context = androidContext()) }
    single<AppPreferences> { AndroidAppPreferences(context = androidContext()) }
    single<SecureStorage> { AndroidSecureStorage(context = androidContext()) }
    single { VisualizerState() }
}
