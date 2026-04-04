package io.github.kiyohitonara.biwa.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.kiyohitonara.biwa.data.local.BiwaDatabase

/** Android implementation using [AndroidSqliteDriver]. */
actual class DatabaseDriverFactory(private val context: Context) {
    /** Returns an [AndroidSqliteDriver] backed by the app's SQLite database. */
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(BiwaDatabase.Schema, context, "biwa.db")
}
