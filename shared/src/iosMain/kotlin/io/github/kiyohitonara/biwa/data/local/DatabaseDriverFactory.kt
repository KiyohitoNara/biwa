package io.github.kiyohitonara.biwa.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.github.kiyohitonara.biwa.data.local.BiwaDatabase

/** iOS implementation using [NativeSqliteDriver]. */
actual class DatabaseDriverFactory {
    /** Returns a [NativeSqliteDriver] backed by the app's SQLite database. */
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(BiwaDatabase.Schema, "biwa.db")
}
