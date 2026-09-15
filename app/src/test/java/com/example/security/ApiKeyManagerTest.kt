package com.example.security

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ApiKeyManagerTest {

    private lateinit var context: Context
    private lateinit var secureStorage: AndroidKeyStoreStorage
    private lateinit var legacyAppPrefs: SharedPreferences
    private lateinit var legacySupabasePrefs: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        secureStorage = AndroidKeyStoreStorage(context, keyAlias = "test_key_alias")
        secureStorage.clear()

        legacyAppPrefs = context.getSharedPreferences("test_legacy_app_prefs", Context.MODE_PRIVATE)
        legacyAppPrefs.edit().clear().apply()

        legacySupabasePrefs = context.getSharedPreferences("test_legacy_sb_prefs", Context.MODE_PRIVATE)
        legacySupabasePrefs.edit().clear().apply()
    }

    @Test
    fun secureStorage_encryptsAndDecryptsAccurately() {
        val testKey = "test_api_token"
        val secretValue = "sk-or-v1-super-secret-token-12345"

        secureStorage.saveSecret(testKey, secretValue)
        assertTrue(secureStorage.hasSecret(testKey))

        val retrieved = secureStorage.getSecret(testKey)
        assertEquals(secretValue, retrieved)

        secureStorage.removeSecret(testKey)
        assertFalse(secureStorage.hasSecret(testKey))
        assertNull(secureStorage.getSecret(testKey))
    }

    @Test
    fun secureStorage_tamperedPayload_returnsNullGracefully() {
        val prefs = context.getSharedPreferences("tessera_secure_vault", Context.MODE_PRIVATE)
        prefs.edit().putString("corrupted_key", "invalid_base64_payload_content!").apply()

        val result = secureStorage.getSecret("corrupted_key")
        assertNull(result)
    }

    @Test
    fun apiKeyManager_migratesLegacyPlainTextKeysAutomatically() {
        val legacyArtespKey = "artesp_legacy_token_abc123"
        val legacySupabaseKey = "sb_legacy_anon_jwt_token_xyz"

        // Simula chaves legadas gravadas em texto puro antes da atualização
        legacyAppPrefs.edit().putString(ApiKeyManager.KEY_ARTESP, legacyArtespKey).apply()
        legacySupabasePrefs.edit().putString(ApiKeyManager.KEY_SUPABASE_ANON, legacySupabaseKey).apply()

        assertTrue(legacyAppPrefs.contains(ApiKeyManager.KEY_ARTESP))
        assertTrue(legacySupabasePrefs.contains(ApiKeyManager.KEY_SUPABASE_ANON))

        // Inicializa o ApiKeyManager, que deve disparar a migração
        val manager = ApiKeyManager(
            secureStorage = secureStorage,
            legacyAppPrefs = legacyAppPrefs,
            legacySupabasePrefs = legacySupabasePrefs
        )

        // Verifica que as chaves agora estão no cofre seguro
        assertEquals(legacyArtespKey, manager.getArtespApiKey())
        assertEquals(legacySupabaseKey, manager.getSupabaseAnonKey())
        assertTrue(secureStorage.hasSecret(ApiKeyManager.KEY_ARTESP))
        assertTrue(secureStorage.hasSecret(ApiKeyManager.KEY_SUPABASE_ANON))

        // Verifica que as chaves em texto puro foram DELETADAS dos SharedPreferences desprotegidos
        assertFalse(legacyAppPrefs.contains(ApiKeyManager.KEY_ARTESP))
        assertFalse(legacySupabasePrefs.contains(ApiKeyManager.KEY_SUPABASE_ANON))
    }

    @Test
    fun apiKeyManager_crudOperations_workAccurately() {
        val manager = ApiKeyManager(
            secureStorage = secureStorage,
            legacyAppPrefs = legacyAppPrefs,
            legacySupabasePrefs = legacySupabasePrefs
        )

        manager.setArtespApiKey("new_artesp_key")
        assertEquals("new_artesp_key", manager.getArtespApiKey())

        manager.removeArtespApiKey()
        assertNull(manager.getArtespApiKey())

        manager.setSupabaseAnonKey("new_sb_key")
        assertEquals("new_sb_key", manager.getSupabaseAnonKey())

        manager.removeSupabaseAnonKey()
        assertNull(manager.getSupabaseAnonKey())
    }
}
