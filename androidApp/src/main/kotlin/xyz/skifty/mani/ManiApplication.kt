package xyz.skifty.mani

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import xyz.skifty.mani.di.androidModule
import xyz.skifty.mani.di.commonModule

class ManiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ManiApplication)
            modules(commonModule, androidModule)
        }
    }

}
