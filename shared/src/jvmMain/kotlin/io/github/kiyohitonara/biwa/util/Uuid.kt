package io.github.kiyohitonara.biwa.util

import java.util.UUID

actual fun generateUuid(): String = UUID.randomUUID().toString()
