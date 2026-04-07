package io.github.kiyohitonara.biwa.data.storage

import io.github.kiyohitonara.biwa.domain.storage.PreferencesStorage
import platform.Foundation.NSUserDefaults

/** [PreferencesStorage] backed by iOS [NSUserDefaults]. */
class NSUserDefaultsStorage : PreferencesStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String, default: String): String =
        defaults.stringForKey(key) ?: default

    override fun setString(key: String, value: String) {
        defaults.setObject(value, key)
    }
}
