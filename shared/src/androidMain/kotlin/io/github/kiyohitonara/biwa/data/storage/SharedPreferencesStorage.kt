package io.github.kiyohitonara.biwa.data.storage

import android.content.Context
import io.github.kiyohitonara.biwa.domain.storage.PreferencesStorage

/** [PreferencesStorage] backed by Android [SharedPreferences]. */
class SharedPreferencesStorage(context: Context) : PreferencesStorage {
    private val prefs = context.getSharedPreferences("biwa_prefs", Context.MODE_PRIVATE)

    override fun getString(key: String, default: String): String =
        prefs.getString(key, default) ?: default

    override fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}
