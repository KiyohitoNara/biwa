package io.github.kiyohitonara.biwa.domain.model

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Represents a user-defined album that can hold any number of media items.
 *
 * Albums form a tree: [parentId] references the containing album, or is null for a
 * root-level album. Media membership is direct only and is never rolled up to ancestors.
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("MediaAlbum")
data class Album(
    val id: String,
    val name: String,
    val createdAt: Long,
    val parentId: String? = null,
)
