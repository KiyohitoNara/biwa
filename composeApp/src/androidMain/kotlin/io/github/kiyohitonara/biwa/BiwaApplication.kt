package io.github.kiyohitonara.biwa

import android.app.Application
import io.github.kiyohitonara.biwa.di.platformModule
import io.github.kiyohitonara.biwa.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** Application entry point. Initializes Koin with platform and shared modules. */
class BiwaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BiwaApplication)
            modules(platformModule, sharedModule)
        }
    }
}
