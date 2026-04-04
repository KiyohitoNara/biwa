package io.github.kiyohitonara.biwa.util

import platform.Foundation.NSDate

actual fun currentEpochSeconds(): Long = NSDate.timeIntervalSince1970.toLong()
