package io.github.kiyohitonara.biwa.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.kiyohitonara.biwa.data.local.BiwaDatabase

/** Android implementation using [AndroidSqliteDriver]. */
actual class DatabaseDriverFactory(
    private val context: Context,
) {
    /**
     * Returns an [AndroidSqliteDriver] backed by the app's SQLite database.
     *
     * Foreign key enforcement is enabled so that ON DELETE CASCADE constraints
     * (media associations and nested album subtrees) take effect.
     */
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = BiwaDatabase.Schema,
            context = context,
            name = "biwa.db",
            callback =
                object : AndroidSqliteDriver.Callback(BiwaDatabase.Schema) {
                    override fun onConfigure(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                },
        )
}
