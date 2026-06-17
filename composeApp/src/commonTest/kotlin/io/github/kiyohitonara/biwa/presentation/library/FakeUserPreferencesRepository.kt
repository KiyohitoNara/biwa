package io.github.kiyohitonara.biwa.presentation.library

import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.domain.model.UserPreferences
import io.github.kiyohitonara.biwa.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [UserPreferencesRepository] for use in presentation-layer tests. */
class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(),
) : UserPreferencesRepository {
    private val prefs = MutableStateFlow(initial)

    override fun getPreferences(): Flow<UserPreferences> = prefs.asStateFlow()

    override suspend fun setTheme(theme: AppTheme) {
        prefs.value = prefs.value.copy(theme = theme)
    }
}
