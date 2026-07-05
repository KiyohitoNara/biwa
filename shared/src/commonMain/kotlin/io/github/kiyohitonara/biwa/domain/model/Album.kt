package io.github.kiyohitonara.biwa.domain.model

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/** Represents a user-defined label that can be attached to any number of media items. */
@OptIn(ExperimentalObjCName::class)
@ObjCName("MediaAlbum")
data class Album(
    val id: String,
    val name: String,
    val createdAt: Long,
)
