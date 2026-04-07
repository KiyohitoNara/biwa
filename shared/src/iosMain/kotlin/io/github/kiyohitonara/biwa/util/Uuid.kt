package io.github.kiyohitonara.biwa.util

import platform.Foundation.NSUUID

actual fun generateUuid(): String = NSUUID().UUIDString
