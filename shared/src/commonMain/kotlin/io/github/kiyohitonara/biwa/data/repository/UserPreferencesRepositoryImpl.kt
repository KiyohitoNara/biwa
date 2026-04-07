package io.github.kiyohitonara.biwa.data.repository

import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.domain.model.SortOrder
import io.github.kiyohitonara.biwa.domain.model.UserPreferences
import io.github.kiyohitonara.biwa.domain.repository.UserPreferencesRepository
import io.github.kiyohitonara.biwa.domain.storage.PreferencesStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [UserPreferencesRepository] backed by [PreferencesStorage]. */
class UserPreferencesRepositoryImpl(
    private val storage: PreferencesStorage,
) : UserPreferencesRepository {
    private val _preferences = MutableStateFlow(load())

    override fun getPreferences(): Flow<UserPreferences> = _preferences.asStateFlow()

    override suspend fun setDefaultSortOrder(sortOrder: SortOrder) {
        storage.setString(KEY_SORT_ORDER, sortOrder.name)
        _preferences.value = load()
    }

    override suspend fun setTheme(theme: AppTheme) {
        storage.setString(KEY_THEME, theme.name)
        _preferences.value = load()
    }

    private fun load(): UserPreferences = UserPreferences(
        defaultSortOrder = storage.getString(KEY_SORT_ORDER, SortOrder.ADDED_AT_DESC.name)
            .let { runCatching { SortOrder.valueOf(it) }.getOrDefault(SortOrder.ADDED_AT_DESC) },
        theme = storage.getString(KEY_THEME, AppTheme.SYSTEM.name)
            .let { runCatching { AppTheme.valueOf(it) }.getOrDefault(AppTheme.SYSTEM) },
    )

    companion object {
        private const val KEY_SORT_ORDER = "default_sort_order"
        private const val KEY_THEME = "theme"
    }
}
