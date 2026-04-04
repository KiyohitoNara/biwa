package io.github.kiyohitonara.biwa.data.local

import app.cash.sqldelight.db.SqlDriver

/** Creates a platform-specific SQLDelight [SqlDriver]. */
expect class DatabaseDriverFactory {
    /** Returns a [SqlDriver] backed by the platform's SQLite implementation. */
    fun createDriver(): SqlDriver
}
