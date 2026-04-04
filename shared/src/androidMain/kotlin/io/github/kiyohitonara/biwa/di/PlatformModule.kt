package io.github.kiyohitonara.biwa.di

import app.cash.sqldelight.db.SqlDriver
import io.github.kiyohitonara.biwa.data.local.DatabaseDriverFactory
import io.github.kiyohitonara.biwa.data.local.FileManager
import io.github.kiyohitonara.biwa.domain.storage.FileStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Koin module for Android-specific bindings that require a [Context]. */
val platformModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single<SqlDriver> { get<DatabaseDriverFactory>().createDriver() }
    single<FileStorage> { FileManager(androidContext()) }
}
