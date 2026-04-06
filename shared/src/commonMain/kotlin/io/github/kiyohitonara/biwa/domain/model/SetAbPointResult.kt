package io.github.kiyohitonara.biwa.domain.model

/** Result of attempting to set an AB-repeat point. */
sealed interface SetAbPointResult {
    /** The point was valid and the state was saved. */
    data object Success : SetAbPointResult

    /** The resulting range would be invalid (B-point ≤ A-point). */
    data object InvalidRange : SetAbPointResult
}
