package io.github.kiyohitonara.biwa

import androidx.compose.ui.window.ComposeUIViewController
import io.github.kiyohitonara.biwa.di.platformModule
import io.github.kiyohitonara.biwa.di.sharedModule
import io.github.kiyohitonara.biwa.di.viewModelModule
import org.koin.compose.KoinApplication

/** iOS entry point that initializes Koin and hosts the shared Compose UI. */
fun MainViewController() = ComposeUIViewController {
    KoinApplication(application = {
        modules(platformModule, sharedModule, viewModelModule)
    }) {
        App()
    }
}
