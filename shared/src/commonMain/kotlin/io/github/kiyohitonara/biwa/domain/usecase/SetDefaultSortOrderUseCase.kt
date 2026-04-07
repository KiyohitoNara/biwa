package io.github.kiyohitonara.biwa.domain.usecase

import io.github.kiyohitonara.biwa.domain.model.SortOrder
import io.github.kiyohitonara.biwa.domain.repository.UserPreferencesRepository

/** Persists [SortOrder] as the default sort order applied on library launch. */
class SetDefaultSortOrderUseCase(private val repository: UserPreferencesRepository) {
    /** Executes the use case with the given [sortOrder]. */
    suspend fun execute(sortOrder: SortOrder) = repository.setDefaultSortOrder(sortOrder)
}
