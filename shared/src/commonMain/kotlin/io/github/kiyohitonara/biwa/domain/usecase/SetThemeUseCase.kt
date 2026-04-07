package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.AppTheme
import io.github.kiyohitonara.biwa.domain.repository.UserPreferencesRepository

/** Persists [AppTheme] as the app-wide color scheme. */
class SetThemeUseCase(private val repository: UserPreferencesRepository) {
    /** Executes the use case with the given [theme]. */
    suspend fun execute(theme: AppTheme) = repository.setTheme(theme)
}
