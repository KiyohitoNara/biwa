package io.github.kiyohitonara.biwa.di

import io.github.kiyohitonara.biwa.presentation.addmedia.AddMediaViewModel
import io.github.kiyohitonara.biwa.presentation.library.LibraryViewModel
import io.github.kiyohitonara.biwa.presentation.photoviewer.PhotoViewerViewModel
import io.github.kiyohitonara.biwa.presentation.videoplayer.VideoPlayerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/** Koin module that registers all ViewModels. */
val viewModelModule = module {
    viewModel { AddMediaViewModel(get(), get()) }
    viewModel { LibraryViewModel(get(), get(), get(), get()) }
    viewModel { params -> VideoPlayerViewModel(params.get(), get(), get(), get(), get(), get()) }
    viewModel { params -> PhotoViewerViewModel(params.get(), get(), get()) }
}
