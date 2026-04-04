package io.github.kiyohitonara.biwa.di

import app.cash.sqldelight.db.SqlDriver
import io.github.kiyohitonara.biwa.data.local.DatabaseDriverFactory
import io.github.kiyohitonara.biwa.data.local.FileManager
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
import org.koin.dsl.module

/** Koin module for iOS-specific bindings. */
val platformModule = module {
    single { DatabaseDriverFactory() }
    single<SqlDriver> { get<DatabaseDriverFactory>().createDriver() }
    single<FileStorage> { FileManager() }
}
