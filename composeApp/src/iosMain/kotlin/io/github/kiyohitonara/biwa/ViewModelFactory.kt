package io.github.kiyohitonara.biwa

import io.github.kiyohitonara.biwa.presentation.library.LibraryViewModel
import io.github.kiyohitonara.biwa.presentation.mediaviewer.MediaViewerViewModel
import io.github.kiyohitonara.biwa.presentation.settings.SettingsViewModel
import io.github.kiyohitonara.biwa.presentation.tagmanagement.TagManagementViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

/** Provides Koin-managed ViewModels to Swift iOS code. */
object ViewModelFactory : KoinComponent {
    /** Returns a new [LibraryViewModel] instance from the Koin container. */
    fun makeLibraryViewModel(): LibraryViewModel = get()

    /** Returns a new [SettingsViewModel] instance from the Koin container. */
    fun makeSettingsViewModel(): SettingsViewModel = get()

    /**
     * Returns a new [MediaViewerViewModel] instance from the Koin container.
     *
     * @param mediaId ID of the media item to show first.
     */
    fun makeMediaViewerViewModel(mediaId: String): MediaViewerViewModel = get { parametersOf(mediaId) }

    /**
     * Returns a new [TagManagementViewModel] instance from the Koin container.
     *
     * Pass [mediaId] to enable per-media tag toggling; pass null for global tag management.
     */
    fun makeTagManagementViewModel(mediaId: String?): TagManagementViewModel = get { parametersOf(mediaId) }
}
