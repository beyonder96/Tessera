package com.example.security

import android.content.SharedPreferences
import android.util.Log

/**
 * Gerenciador centralizado de chaves de API e credenciais confidenciais do Tessera.
 * 
 * Funcionalidades:
 * - Armazena tokens em cofre seguro criptografado por hardware (SecureStorage).
 * - Executa migração automática e transparente de chaves legadas que antes eram gravadas
 *   em texto puro no SharedPreferences ("tessera_prefs" e "tessera_supabase_prefs").
 * - Remove as versões em texto claro do armazenamento desprotegido assim que migradas.
 */
class ApiKeyManager(
    private val secureStorage: SecureStorage,
    private val legacyAppPrefs: SharedPreferences,
    private val legacySupabasePrefs: SharedPreferences? = null
) {
    companion object {
        private const val TAG = "ApiKeyManager"
        const val KEY_ARTESP = "artesp_api_key"
        const val KEY_SUPABASE_ANON = "supabase_anon_key"
        const val KEY_FOOTBALL = "football_api_key"
    }

    init {
        migrateLegacyKeys()
    }

    /**
     * Varre os SharedPreferences desprotegidos e move chaves existentes
     * para o cofre seguro, apagando a cópia desprotegida.
     */
    private fun migrateLegacyKeys() {
        try {
            // 1. Migração da chave da ARTESP
            if (!secureStorage.hasSecret(KEY_ARTESP) && legacyAppPrefs.contains(KEY_ARTESP)) {
                val plainKey = legacyAppPrefs.getString(KEY_ARTESP, null)?.trim()
                if (!plainKey.isNullOrBlank()) {
                    secureStorage.saveSecret(KEY_ARTESP, plainKey)
                    Log.i(TAG, "Chave ARTESP migrada com sucesso para o cofre seguro.")
                }
                legacyAppPrefs.edit().remove(KEY_ARTESP).apply()
            }

            // 2. Migração da chave do Supabase
            legacySupabasePrefs?.let { sbPrefs ->
                if (!secureStorage.hasSecret(KEY_SUPABASE_ANON) && sbPrefs.contains(KEY_SUPABASE_ANON)) {
                    val plainKey = sbPrefs.getString(KEY_SUPABASE_ANON, null)?.trim()
                    if (!plainKey.isNullOrBlank()) {
                        secureStorage.saveSecret(KEY_SUPABASE_ANON, plainKey)
                        Log.i(TAG, "Chave do Supabase migrada com sucesso para o cofre seguro.")
                    }
                    sbPrefs.edit().remove(KEY_SUPABASE_ANON).apply()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante migração de chaves legadas", e)
        }
    }

    // --- ARTESP API Key ---

    fun getArtespApiKey(): String? {
        return secureStorage.getSecret(KEY_ARTESP)?.takeIf { it.isNotBlank() }
            ?: legacyAppPrefs.getString(KEY_ARTESP, null)?.takeIf { it.isNotBlank() }
    }

    fun setArtespApiKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isNotBlank()) {
            secureStorage.saveSecret(KEY_ARTESP, trimmed)
        } else {
            secureStorage.removeSecret(KEY_ARTESP)
        }
        legacyAppPrefs.edit().remove(KEY_ARTESP).apply()
    }

    fun removeArtespApiKey() {
        secureStorage.removeSecret(KEY_ARTESP)
        legacyAppPrefs.edit().remove(KEY_ARTESP).apply()
    }

    // --- Supabase Anon Key ---

    fun getSupabaseAnonKey(): String? {
        return secureStorage.getSecret(KEY_SUPABASE_ANON)?.takeIf { it.isNotBlank() }
            ?: legacySupabasePrefs?.getString(KEY_SUPABASE_ANON, null)?.takeIf { it.isNotBlank() }
    }

    fun setSupabaseAnonKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isNotBlank()) {
            secureStorage.saveSecret(KEY_SUPABASE_ANON, trimmed)
        } else {
            secureStorage.removeSecret(KEY_SUPABASE_ANON)
        }
        legacySupabasePrefs?.edit()?.remove(KEY_SUPABASE_ANON)?.apply()
    }

    fun removeSupabaseAnonKey() {
        secureStorage.removeSecret(KEY_SUPABASE_ANON)
        legacySupabasePrefs?.edit()?.remove(KEY_SUPABASE_ANON)?.apply()
    }

    // --- Genérico para futuras credenciais ---

    fun getSecret(key: String): String? = secureStorage.getSecret(key)

    fun setSecret(key: String, value: String) {
        val trimmed = value.trim()
        if (trimmed.isNotBlank()) {
            secureStorage.saveSecret(key, trimmed)
        } else {
            secureStorage.removeSecret(key)
        }
    }

    fun removeSecret(key: String) = secureStorage.removeSecret(key)
}
