package io.github.kiyohitonara.biwa.di

import io.github.kiyohitonara.biwa.data.repository.MediaRepositoryImpl
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import io.github.kiyohitonara.biwa.domain.usecase.AddMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
import io.github.kiyohitonara.biwa.util.currentEpochSeconds
import org.koin.dsl.module

/** Koin module for platform-agnostic bindings shared across all targets. */
val sharedModule = module {
    single<MediaRepository> { MediaRepositoryImpl(get()) }
    factory { AddMediaUseCase(get(), get(), clock = { currentEpochSeconds() }) }
    factory { GetAllMediaUseCase(get()) }
    factory { DeleteMediaUseCase(get(), get()) }
    factory { GetMediaByIdUseCase(get()) }
    factory { UpdateLastViewedAtUseCase(get(), clock = { currentEpochSeconds() }) }
}
