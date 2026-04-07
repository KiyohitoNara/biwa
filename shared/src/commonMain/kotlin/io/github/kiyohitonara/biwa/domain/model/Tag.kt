package io.github.kiyohitonara.biwa.domain.model

/** Represents a user-defined label that can be attached to any number of media items. */
data class Tag(
    val id: String,
    val name: String,
    val createdAt: Long,
)
