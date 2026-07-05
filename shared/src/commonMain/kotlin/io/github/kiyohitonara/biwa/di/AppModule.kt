package io.github.kiyohitonara.biwa.di

import io.github.kiyohitonara.biwa.data.repository.AlbumRepositoryImpl
import io.github.kiyohitonara.biwa.data.repository.MediaRepositoryImpl
import io.github.kiyohitonara.biwa.data.repository.PlaybackStateRepositoryImpl
import io.github.kiyohitonara.biwa.data.repository.UserPreferencesRepositoryImpl
import io.github.kiyohitonara.biwa.domain.repository.AlbumRepository
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository
import io.github.kiyohitonara.biwa.domain.repository.UserPreferencesRepository
import io.github.kiyohitonara.biwa.domain.usecase.AddMediaToAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.AddMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.CreateAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GenerateThumbnailUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAlbumsForMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllAlbumsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllPhotosUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaIdsInAllAlbumsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetOrderedMediaIdsForAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetPlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetUserPreferencesUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RemoveMediaFromAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RenameAlbumUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderAlbumMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ResetAbRepeatUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SavePlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetAbPointUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetThemeUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
import io.github.kiyohitonara.biwa.util.currentEpochSeconds
import io.github.kiyohitonara.biwa.util.generateUuid
import org.koin.dsl.module

/** Koin module for platform-agnostic bindings shared across all targets. */
val sharedModule =
    module {
        single<MediaRepository> { MediaRepositoryImpl(get(), get()) }
        single<PlaybackStateRepository> { PlaybackStateRepositoryImpl(get(), get()) }
        single<AlbumRepository> { AlbumRepositoryImpl(get(), get()) }
        single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(get(), get()) }
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
        factory { GetAllAlbumsUseCase(get()) }
        factory { CreateAlbumUseCase(get(), idGenerator = { generateUuid() }, clock = { currentEpochSeconds() }) }
        factory { RenameAlbumUseCase(get()) }
        factory { DeleteAlbumUseCase(get()) }
        factory { GetAlbumsForMediaUseCase(get()) }
        factory { AddMediaToAlbumUseCase(get()) }
        factory { RemoveMediaFromAlbumUseCase(get()) }
        factory { GetMediaIdsInAllAlbumsUseCase(get()) }
        factory { GetOrderedMediaIdsForAlbumUseCase(get()) }
        factory { ReorderAlbumMediaUseCase(get()) }
        factory { GetUserPreferencesUseCase(get()) }
        factory { SetThemeUseCase(get()) }
    }
