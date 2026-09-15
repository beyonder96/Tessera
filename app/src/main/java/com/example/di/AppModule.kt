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
import com.example.security.AndroidKeyStoreStorage
import com.example.security.ApiKeyManager
import com.example.security.SecureStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val securityModule = module {
    single<SecureStorage> { AndroidKeyStoreStorage(androidContext()) }
    single {
        ApiKeyManager(
            secureStorage = get(),
            legacyAppPrefs = get(),
            legacySupabasePrefs = androidContext().getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
        )
    }
}

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
    viewModel { TesseraViewModel(get(), androidContext(), get()) }
}

val allAppModules = listOf(
    securityModule,
    databaseModule,
    networkModule,
    appModule,
    viewModelModule
)
