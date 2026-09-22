package com.hakki.nextgallery.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the Nextcloud server URL, username, app password and photos folder.
 * Backed by EncryptedSharedPreferences so the app password isn't sitting in plaintext.
 */
class Prefs(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "nextgallery_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SERVER, value.trimEnd('/')).apply()

    var username: String
        get() = prefs.getString(KEY_USER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER, value).apply()

    var appPassword: String
        get() = prefs.getString(KEY_PASS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASS, value).apply()

    /** Folder to browse, relative to the user's files root. Empty = whole account. */
    var photosFolder: String
        get() = prefs.getString(KEY_FOLDER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FOLDER, value.trim('/')).apply()

    val isLoggedIn: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && appPassword.isNotBlank()

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_SERVER = "server_url"
        private const val KEY_USER = "username"
        private const val KEY_PASS = "app_password"
        private const val KEY_FOLDER = "photos_folder"
    }
}
