package io.github.kiyohitonara.biwa.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.github.kiyohitonara.biwa.data.local.BiwaDatabase

/** iOS implementation using [NativeSqliteDriver]. */
actual class DatabaseDriverFactory {
    /**
     * Returns a [NativeSqliteDriver] backed by the app's SQLite database.
     *
     * Foreign key enforcement is enabled so that ON DELETE CASCADE constraints
     * (media associations and nested album subtrees) take effect.
     */
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(
            schema = BiwaDatabase.Schema,
            name = "biwa.db",
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = config.extendedConfig.copy(foreignKeyConstraints = true),
                )
            },
        )
}
