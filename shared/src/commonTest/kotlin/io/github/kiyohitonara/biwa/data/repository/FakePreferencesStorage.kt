package io.github.kiyohitonara.biwa.data.repository

import io.github.kiyohitonara.biwa.domain.storage.PreferencesStorage

/** In-memory [PreferencesStorage] for use in tests. */
class FakePreferencesStorage : PreferencesStorage {
    private val values = mutableMapOf<String, String>()

    override fun getString(
        key: String,
        default: String,
    ): String = values[key] ?: default

    override fun setString(
        key: String,
        value: String,
    ) {
        values[key] = value
    }
}
