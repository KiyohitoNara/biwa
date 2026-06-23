package io.github.kiyohitonara.biwa.data.repository

import co.touchlab.kermit.Logger
import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.domain.model.UserPreferences
import io.github.kiyohitonara.biwa.domain.repository.UserPreferencesRepository
import io.github.kiyohitonara.biwa.domain.storage.PreferencesStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [UserPreferencesRepository] backed by [PreferencesStorage]. */
class UserPreferencesRepositoryImpl(
    private val storage: PreferencesStorage,
    logger: Logger,
) : UserPreferencesRepository {
    private val log = logger.withTag("UserPreferencesRepository")
    private val _preferences = MutableStateFlow(load())

    override fun getPreferences(): Flow<UserPreferences> = _preferences.asStateFlow()

    override suspend fun setTheme(theme: AppTheme) {
        log.d { "setTheme theme=$theme" }
        storage.setString(KEY_THEME, theme.name)
        _preferences.value = load()
    }

    private fun load(): UserPreferences =
        UserPreferences(
            theme =
                storage
                    .getString(KEY_THEME, AppTheme.SYSTEM.name)
                    .let { runCatching { AppTheme.valueOf(it) }.getOrDefault(AppTheme.SYSTEM) },
        )

    companion object {
        private const val KEY_THEME = "theme"
    }
}
