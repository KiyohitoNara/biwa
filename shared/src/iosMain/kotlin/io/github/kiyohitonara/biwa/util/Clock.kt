package io.github.kiyohitonara.biwa.util

import platform.Foundation.NSDate

// NSDate reference date is Jan 1, 2001; Unix epoch is Jan 1, 1970 (difference = 978307200 s).
actual fun currentEpochSeconds(): Long =
    (NSDate().timeIntervalSinceReferenceDate + 978307200.0).toLong()
