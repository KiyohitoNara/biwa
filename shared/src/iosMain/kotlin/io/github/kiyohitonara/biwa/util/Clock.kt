package io.github.kiyohitonara.biwa.util

import platform.Foundation.NSDate

// NSDate reference date is Jan 1, 2001; Unix epoch is Jan 1, 1970.
private const val REFERENCE_DATE_TO_EPOCH_OFFSET_SECONDS = 978_307_200.0

actual fun currentEpochSeconds(): Long = (NSDate().timeIntervalSinceReferenceDate + REFERENCE_DATE_TO_EPOCH_OFFSET_SECONDS).toLong()
