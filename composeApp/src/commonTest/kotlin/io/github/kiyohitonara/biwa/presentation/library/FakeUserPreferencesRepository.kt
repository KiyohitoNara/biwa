package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.domain.model.SortOrder
import io.github.kiyohitonara.biwa.domain.model.UserPreferences
import io.github.kiyohitonara.biwa.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [UserPreferencesRepository] for use in tests. */
class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(),
) : UserPreferencesRepository {
    private val _prefs = MutableStateFlow(initial)

    override fun getPreferences(): Flow<UserPreferences> = _prefs.asStateFlow()

    override suspend fun setDefaultSortOrder(sortOrder: SortOrder) {
        _prefs.value = _prefs.value.copy(defaultSortOrder = sortOrder)
    }

    override suspend fun setTheme(theme: AppTheme) {
        _prefs.value = _prefs.value.copy(theme = theme)
    }
}
