package io.github.kiyohitonara.biwa.data.local

import app.cash.sqldelight.db.SqlDriver

/** JVM stub — not used at runtime; real driver is injected per platform. */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = error("DatabaseDriverFactory is not supported on JVM")
}
