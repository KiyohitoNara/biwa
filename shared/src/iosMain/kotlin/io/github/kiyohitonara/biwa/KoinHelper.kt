package io.github.kiyohitonara.biwa

import io.github.kiyohitonara.biwa.di.loggingModule
import io.github.kiyohitonara.biwa.di.platformModule
import io.github.kiyohitonara.biwa.di.sharedModule
import io.github.kiyohitonara.biwa.di.viewModelModule
import org.koin.core.context.startKoin
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/** Starts the Koin DI container. Call once from the Swift app entry point before any screen is shown. */
object KoinHelper {
    @OptIn(ExperimentalNativeApi::class)
    fun start() {
        startKoin {
            modules(loggingModule(Platform.isDebugBinary), platformModule, sharedModule, viewModelModule)
        }
    }
}
