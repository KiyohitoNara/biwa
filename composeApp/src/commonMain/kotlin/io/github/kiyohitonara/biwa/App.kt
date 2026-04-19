package io.github.kiyohitonara.biwa

import androidx.compose.runtime.Composable

/** Root composable for the application. Platform-specific implementations provide navigation. */
@Composable
expect fun App()
