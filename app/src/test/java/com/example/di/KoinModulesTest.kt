package com.example.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.TesseraRepository
import com.example.viewmodel.ApartmentViewModel
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.PetViewModel
import com.example.feature.focus.PomodoroViewModel
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KoinModulesTest : KoinTest {



    @Test
    fun databaseAndRepository_areResolvedSuccessfully() {
        val db: AppDatabase = get()
        val repo: TesseraRepository = get()
        assertNotNull(db)
        assertNotNull(repo)
    }

    @Test
    fun viewModels_areResolvedSuccessfully() {
        val apartmentViewModel: ApartmentViewModel = get()
        val pomodoroViewModel: PomodoroViewModel = get()
        val petViewModel: PetViewModel = get()
        val chatViewModel: ChatViewModel = get()

        assertNotNull(apartmentViewModel)
        assertNotNull(pomodoroViewModel)
        assertNotNull(petViewModel)
        assertNotNull(chatViewModel)
    }

    @Test
    fun securityServices_areResolvedSuccessfully() {
        val secureStorage: com.example.security.SecureStorage = get()
        val apiKeyManager: com.example.security.ApiKeyManager = get()
        val tesseraViewModel: com.example.viewmodel.TesseraViewModel = get()

        assertNotNull(secureStorage)
        assertNotNull(apiKeyManager)
        assertNotNull(tesseraViewModel)
    }
}
