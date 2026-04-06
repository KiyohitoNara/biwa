package io.github.kiyohitonara.biwa.di

import io.github.kiyohitonara.biwa.data.repository.MediaRepositoryImpl
import io.github.kiyohitonara.biwa.data.repository.PlaybackStateRepositoryImpl
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository
import io.github.kiyohitonara.biwa.domain.usecase.AddMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GenerateThumbnailUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllPhotosUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetPlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ResetAbRepeatUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SavePlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetAbPointUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
import io.github.kiyohitonara.biwa.util.currentEpochSeconds
import org.koin.dsl.module

/** Koin module for platform-agnostic bindings shared across all targets. */
val sharedModule = module {
    single<MediaRepository> { MediaRepositoryImpl(get()) }
    single<PlaybackStateRepository> { PlaybackStateRepositoryImpl(get()) }
    factory { AddMediaUseCase(get(), get(), clock = { currentEpochSeconds() }) }
    factory { GetAllMediaUseCase(get()) }
    factory { DeleteMediaUseCase(get(), get()) }
    factory { GenerateThumbnailUseCase(get(), get()) }
    factory { ReorderMediaUseCase(get()) }
    factory { GetMediaByIdUseCase(get()) }
    factory { GetAllPhotosUseCase(get()) }
    factory { UpdateLastViewedAtUseCase(get(), clock = { currentEpochSeconds() }) }
    factory { GetPlaybackStateUseCase(get()) }
    factory { SavePlaybackStateUseCase(get(), clock = { currentEpochSeconds() }) }
    factory { SetAbPointUseCase(get(), clock = { currentEpochSeconds() }) }
    factory { ResetAbRepeatUseCase(get(), clock = { currentEpochSeconds() }) }
}
