package com.example.di

import android.content.Context
import android.content.SharedPreferences
import com.example.data.AppDatabase
import com.example.data.TesseraRepository
import com.example.data.apifootball.NetworkModule
import com.example.viewmodel.ApartmentViewModel
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.FocusSoundPlayer
import com.example.viewmodel.PetViewModel
import com.example.viewmodel.PomodoroViewModel
import com.example.viewmodel.TesseraViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().tesseraDao() }
    single { TesseraRepository(get()) }
}

val networkModule = module {
    single { NetworkModule.provideApiFootballService() }
    single { NetworkModule.provideApiFootballRepository(get()) }
}

val appModule = module {
    single<SharedPreferences> {
        androidContext().getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)
    }
    single { FocusSoundPlayer() }
}

val viewModelModule = module {
    viewModel { ApartmentViewModel(get()) }
    viewModel { PomodoroViewModel(get()) }
    viewModel { PetViewModel(get()) }
    viewModel { ChatViewModel() }
    viewModel { TesseraViewModel(get(), androidContext()) }
}

val allAppModules = listOf(
    databaseModule,
    networkModule,
    appModule,
    viewModelModule
)
