package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.UserPreferences
import io.github.kiyohitonara.biwa.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

/** Returns a reactive stream of the current user preferences. */
class GetUserPreferencesUseCase(private val repository: UserPreferencesRepository) {
    /** Executes the use case. Emits immediately on collection. */
    fun execute(): Flow<UserPreferences> = repository.getPreferences()
}
