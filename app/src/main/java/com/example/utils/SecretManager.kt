package com.example.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecretManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "myraa_secrets",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString("gemini_api_key", apiKey).apply()
    }

    fun getApiKey(): String? {
        return sharedPreferences.getString("gemini_api_key", null)
    }

    fun removeApiKey() {
        sharedPreferences.edit().remove("gemini_api_key").apply()
    }
}
