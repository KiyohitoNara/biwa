package io.github.kiyohitonara.biwa.domain.storage

/**
 * Port for synchronous key-value preference storage.
 *
 * Implementations back this with platform-specific storage
 * (SharedPreferences on Android, NSUserDefaults on iOS).
 */
interface PreferencesStorage {
    /** Returns the [String] value for [key], or [default] if not set. */
    fun getString(key: String, default: String): String

    /** Persists [value] for [key]. */
    fun setString(key: String, value: String)
}
