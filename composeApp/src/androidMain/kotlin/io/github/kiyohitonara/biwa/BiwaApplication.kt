package io.github.kiyohitonara.biwa

import android.app.Application
import android.content.pm.ApplicationInfo
import co.touchlab.kermit.platformLogWriter
import io.github.kiyohitonara.biwa.di.loggingModule
import io.github.kiyohitonara.biwa.di.platformModule
import io.github.kiyohitonara.biwa.di.sharedModule
import io.github.kiyohitonara.biwa.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** Application entry point. Initializes Koin with platform and shared modules. */
class BiwaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        startKoin {
            androidContext(this@BiwaApplication)
            modules(loggingModule(isDebug, platformLogWriter()), platformModule, sharedModule, viewModelModule)
        }
    }
}
