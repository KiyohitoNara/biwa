package io.github.kiyohitonara.biwa.di

import io.github.kiyohitonara.biwa.presentation.library.LibraryViewModel
import io.github.kiyohitonara.biwa.presentation.mediaviewer.MediaViewerViewModel
import io.github.kiyohitonara.biwa.presentation.settings.SettingsViewModel
import io.github.kiyohitonara.biwa.presentation.tagmanagement.TagManagementViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Koin module that registers all ViewModels. */
val viewModelModule = module {
    viewModel { LibraryViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { params -> MediaViewerViewModel(params.get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { params -> TagManagementViewModel(params.get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
}
