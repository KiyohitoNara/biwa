package io.github.kiyohitonara.biwa.di

import io.github.kiyohitonara.biwa.presentation.addmedia.AddMediaViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Koin module that registers all ViewModels. */
val viewModelModule = module {
    viewModel { AddMediaViewModel(get(), get()) }
}
