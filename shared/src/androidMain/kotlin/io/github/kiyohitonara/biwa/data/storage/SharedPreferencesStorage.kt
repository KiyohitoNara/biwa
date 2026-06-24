package io.github.kiyohitonara.biwa.data.storage

import android.content.Context
import co.touchlab.kermit.Logger
import io.github.kiyohitonara.biwa.domain.storage.PreferencesStorage

/** [PreferencesStorage] backed by Android [SharedPreferences]. */
class SharedPreferencesStorage(
    context: Context,
    logger: Logger,
) : PreferencesStorage {
    private val log = logger.withTag("PreferencesStorage")
    private val prefs = context.getSharedPreferences("biwa_prefs", Context.MODE_PRIVATE)

    override fun getString(
        key: String,
        default: String,
    ): String = prefs.getString(key, default) ?: default

    override fun setString(
        key: String,
        value: String,
    ) {
        log.v { "setString key=$key value=$value" }
        prefs.edit().putString(key, value).apply()
    }
}
