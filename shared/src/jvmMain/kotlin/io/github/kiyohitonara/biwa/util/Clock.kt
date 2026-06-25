package io.github.kiyohitonara.biwa.util

private const val MILLIS_PER_SECOND = 1000L

actual fun currentEpochSeconds(): Long = System.currentTimeMillis() / MILLIS_PER_SECOND
