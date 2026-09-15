package com.example.feature.focus.di

import com.example.feature.focus.FocusSoundPlayer
import com.example.feature.focus.PomodoroViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val focusModule = module {
    single { FocusSoundPlayer() }
    viewModel { PomodoroViewModel(get()) }
}
