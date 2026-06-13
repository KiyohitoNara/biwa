package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.domain.model.UserPreferences
import io.github.kiyohitonara.biwa.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserPreferencesUseCaseTest {
    private val repository = FakeUserPreferencesRepository()
    private val getUseCase = GetUserPreferencesUseCase(repository)
    private val setThemeUseCase = SetThemeUseCase(repository)

    @Test
    fun `GetUserPreferencesUseCase emits default preferences initially`() = runTest {
        val prefs = getUseCase.execute().first()
        assertEquals(AppTheme.SYSTEM, prefs.theme)
    }

    @Test
    fun `SetThemeUseCase persists theme`() = runTest {
        setThemeUseCase.execute(AppTheme.DARK)

        val prefs = getUseCase.execute().first()
        assertEquals(AppTheme.DARK, prefs.theme)
    }
}

private class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val _prefs = MutableStateFlow(UserPreferences())

    override fun getPreferences(): Flow<UserPreferences> = _prefs.asStateFlow()

    override suspend fun setTheme(theme: AppTheme) {
        _prefs.value = _prefs.value.copy(theme = theme)
    }
}
