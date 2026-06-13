package io.github.kiyohitonara.biwa.domain.repository

import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/** Provides access to persisted user preferences. */
interface UserPreferencesRepository {
    /** Returns a flow of the current preferences. Emits immediately on collection. */
    fun getPreferences(): Flow<UserPreferences>

    /** Persists [theme] as the app-wide color scheme. */
    suspend fun setTheme(theme: AppTheme)
}
