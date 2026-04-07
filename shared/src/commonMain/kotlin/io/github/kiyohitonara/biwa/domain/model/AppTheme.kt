package io.github.kiyohitonara.biwa.domain.model

/** Controls the color scheme applied to the entire app. */
enum class AppTheme {
    /** Follows the device's system setting. */
    SYSTEM,

    /** Forces light mode regardless of the system setting. */
    LIGHT,

    /** Forces dark mode regardless of the system setting. */
    DARK,
}
