package io.github.kiyohitonara.biwa.data.repository

import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import io.github.kiyohitonara.biwa.domain.model.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserPreferencesRepositoryImplTest {
    private val storage = FakePreferencesStorage()
    private val repository = UserPreferencesRepositoryImpl(storage, Logger(loggerConfigInit()))

    @Test
    fun `getPreferences defaults to SYSTEM theme when nothing is stored`() =
        runTest {
            val preferences = repository.getPreferences().first()

            assertEquals(AppTheme.SYSTEM, preferences.theme)
        }

    @Test
    fun `setTheme persists the theme and updates getPreferences`() =
        runTest {
            repository.setTheme(AppTheme.DARK)

            val preferences = repository.getPreferences().first()
            assertEquals(AppTheme.DARK, preferences.theme)
        }

    @Test
    fun `getPreferences falls back to SYSTEM for a corrupted stored value`() =
        runTest {
            storage.setString("theme", "NOT_A_VALID_THEME")

            val repositoryOverCorruptedStorage = UserPreferencesRepositoryImpl(storage, Logger(loggerConfigInit()))

            val preferences = repositoryOverCorruptedStorage.getPreferences().first()
            assertEquals(AppTheme.SYSTEM, preferences.theme)
        }
}
