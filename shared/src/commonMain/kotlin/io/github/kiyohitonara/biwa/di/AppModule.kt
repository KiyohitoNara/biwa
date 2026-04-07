package io.github.kiyohitonara.biwa.di

import io.github.kiyohitonara.biwa.data.repository.MediaRepositoryImpl
import io.github.kiyohitonara.biwa.data.repository.PlaybackStateRepositoryImpl
import io.github.kiyohitonara.biwa.data.repository.TagRepositoryImpl
import io.github.kiyohitonara.biwa.data.repository.UserPreferencesRepositoryImpl
import io.github.kiyohitonara.biwa.domain.repository.MediaRepository
import io.github.kiyohitonara.biwa.domain.repository.PlaybackStateRepository
import io.github.kiyohitonara.biwa.domain.repository.TagRepository
import io.github.kiyohitonara.biwa.domain.repository.UserPreferencesRepository
import io.github.kiyohitonara.biwa.domain.usecase.AddMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.AddTagToMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.CreateTagUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.DeleteTagUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GenerateThumbnailUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllPhotosUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetAllTagsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaByIdUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetMediaIdsWithAllTagsUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetUserPreferencesUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetDefaultSortOrderUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetThemeUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetPlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.GetTagsForMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RemoveTagFromMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.RenameTagUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ReorderMediaUseCase
import io.github.kiyohitonara.biwa.domain.usecase.ResetAbRepeatUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SavePlaybackStateUseCase
import io.github.kiyohitonara.biwa.domain.usecase.SetAbPointUseCase
import io.github.kiyohitonara.biwa.domain.usecase.UpdateLastViewedAtUseCase
import io.github.kiyohitonara.biwa.util.currentEpochSeconds
import io.github.kiyohitonara.biwa.util.generateUuid
import org.koin.dsl.module

/** Koin module for platform-agnostic bindings shared across all targets. */
val sharedModule = module {
    single<MediaRepository> { MediaRepositoryImpl(get()) }
    single<PlaybackStateRepository> { PlaybackStateRepositoryImpl(get()) }
    single<TagRepository> { TagRepositoryImpl(get()) }
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(get()) }
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
    factory { GetAllTagsUseCase(get()) }
    factory { CreateTagUseCase(get(), idGenerator = { generateUuid() }, clock = { currentEpochSeconds() }) }
    factory { RenameTagUseCase(get()) }
    factory { DeleteTagUseCase(get()) }
    factory { GetTagsForMediaUseCase(get()) }
    factory { AddTagToMediaUseCase(get()) }
    factory { RemoveTagFromMediaUseCase(get()) }
    factory { GetMediaIdsWithAllTagsUseCase(get()) }
    factory { GetUserPreferencesUseCase(get()) }
    factory { SetDefaultSortOrderUseCase(get()) }
    factory { SetThemeUseCase(get()) }
}
