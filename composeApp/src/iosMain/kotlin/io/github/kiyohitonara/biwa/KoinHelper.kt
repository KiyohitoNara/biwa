package io.github.kiyohitonara.biwa

import io.github.kiyohitonara.biwa.di.platformModule
import io.github.kiyohitonara.biwa.di.sharedModule
import io.github.kiyohitonara.biwa.di.viewModelModule
import org.koin.core.context.startKoin

/** Starts the Koin DI container. Call once from the Swift app entry point before any screen is shown. */
object KoinHelper {
    fun start() {
        startKoin {
            modules(platformModule, sharedModule, viewModelModule)
        }
    }
}
