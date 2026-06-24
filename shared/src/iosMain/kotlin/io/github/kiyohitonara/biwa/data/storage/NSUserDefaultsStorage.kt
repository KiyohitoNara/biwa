package io.github.kiyohitonara.biwa.data.storage

import co.touchlab.kermit.Logger
import io.github.kiyohitonara.biwa.domain.storage.PreferencesStorage
import platform.Foundation.NSUserDefaults

/** [PreferencesStorage] backed by iOS [NSUserDefaults]. */
class NSUserDefaultsStorage(
    logger: Logger,
) : PreferencesStorage {
    private val log = logger.withTag("PreferencesStorage")
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(
        key: String,
        default: String,
    ): String = defaults.stringForKey(key) ?: default

    override fun setString(
        key: String,
        value: String,
    ) {
        log.v { "setString key=$key value=$value" }
        defaults.setObject(value, key)
    }
}
