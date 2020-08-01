package com.uoooo.securestorage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

actual class SecureStorage(context: Context, storageName: String = context.packageName + "_preferences") {
    private val encryptedSharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, storageName, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    actual fun put(key: String, value: String) {
        encryptedSharedPreferences.edit().putString(key, value).apply()
    }

    actual fun get(key: String, default: String?): String? {
        return encryptedSharedPreferences.getString(key, default)
    }

    actual fun remove(key: String) {
        encryptedSharedPreferences.edit().remove(key).apply()
    }
}