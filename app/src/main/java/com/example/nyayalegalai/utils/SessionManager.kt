package com.example.nyayalegalai.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.nyayalegalai.database.AppDatabase
import com.example.nyayalegalai.models.SavedAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class LocalUser(
    val uid: String,
    val email: String,
    val name: String,
    val role: String, // "user" or "lawyer"
    val phone: String,
    val barId: String? = null
)

class SessionManager(private val context: Context) {
    private val gson = Gson()
    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "nyaya_legal_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        android.util.Log.e("NYAYA_CRASH_DEBUG", "SessionManager: EncryptedSharedPreferences creation failed. Falling back to plain SharedPreferences.", e)
        try {
            context.deleteSharedPreferences("nyaya_legal_session")
        } catch (ex: Exception) {
            android.util.Log.w("NYAYA_CRASH_DEBUG", "SessionManager: Failed to delete backing shared preferences file", ex)
        }
        context.getSharedPreferences("nyaya_legal_session_fallback", Context.MODE_PRIVATE)
    }

    fun saveUser(user: LocalUser, password: String? = null) {
        android.util.Log.d("NYAYA_CRASH_DEBUG", "SessionManager: saveUser called - uid=${user.uid}, email=${user.email}, role=${user.role}")
        val userJson = gson.toJson(user)
        sharedPreferences.edit()
            .putString("logged_in_user", userJson)
            .putBoolean("is_logged_in", true)
            .apply()

        // Also save to account picker list
        saveToAccountPicker(user, password)
    }

    private fun saveToAccountPicker(user: LocalUser, password: String? = null) {
        val savedAccounts = getSavedAccounts().toMutableList()
        val existingIndex = savedAccounts.indexOfFirst { it.uid == user.uid || it.email == user.email }

        val newAccount = SavedAccount(
            uid = user.uid,
            email = user.email,
            name = user.name,
            role = user.role,
            password = password,
            lastLoginTime = System.currentTimeMillis()
        )

        if (existingIndex != -1) {
            savedAccounts[existingIndex] = newAccount
        } else {
            savedAccounts.add(newAccount)
        }

        val accountsJson = gson.toJson(savedAccounts)
        sharedPreferences.edit().putString("saved_accounts", accountsJson).apply()
    }

    fun getSavedAccounts(): List<SavedAccount> {
        val accountsJson = sharedPreferences.getString("saved_accounts", null) ?: return emptyList()
        val type = object : TypeToken<List<SavedAccount>>() {}.type
        return try {
            gson.fromJson(accountsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun removeAccount(email: String) {
        val savedAccounts = getSavedAccounts().filter { it.email != email }
        val accountsJson = gson.toJson(savedAccounts)
        sharedPreferences.edit().putString("saved_accounts", accountsJson).apply()
    }

    fun clearAllAccounts() {
        sharedPreferences.edit().remove("saved_accounts").apply()
    }

    fun getUser(): LocalUser? {
        val userJson = sharedPreferences.getString("logged_in_user", null)
        android.util.Log.d("NYAYA_CRASH_DEBUG", "SessionManager: getUser loaded JSON=${userJson ?: "Null"}")
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, LocalUser::class.java)
            } catch (e: Exception) {
                android.util.Log.e("NYAYA_CRASH_DEBUG", "SessionManager: getUser parse error", e)
                null
            }
        } else null
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("is_logged_in", false) && FirebaseAuth.getInstance().currentUser != null
    }

    fun isFirstTime(): Boolean {
        return sharedPreferences.getBoolean("is_first_time", true)
    }

    fun setFirstTimeCompleted() {
        sharedPreferences.edit().putBoolean("is_first_time", false).apply()
    }

    fun setLanguage(lang: String) {
        sharedPreferences.edit().putString("app_language", lang).apply()
    }

    fun getLanguage(): String? {
        return sharedPreferences.getString("app_language", "en")
    }

    fun setDarkMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun isDarkMode(): Boolean {
        return sharedPreferences.getBoolean("dark_mode", false)
    }

    fun setThemeColor(color: String) {
        sharedPreferences.edit().putString("theme_color", color).apply()
    }

    fun getThemeColor(): String {
        return sharedPreferences.getString("theme_color", "Default") ?: "Default"
    }

    fun setFontColor(color: String) {
        sharedPreferences.edit().putString("font_color", color).apply()
    }

    fun getFontColor(): String {
        return sharedPreferences.getString("font_color", "Default") ?: "Default"
    }

    fun setUserProfilePhoto(uid: String, photoUrl: String) {
        if (uid.isNotBlank()) {
            sharedPreferences.edit().putString("user_profile_photo_$uid", photoUrl).apply()
        }
    }

    fun getUserProfilePhoto(uid: String): String {
        if (uid.isBlank()) return ""
        return sharedPreferences.getString("user_profile_photo_$uid", "") ?: ""
    }

    suspend fun clearLocalCacheDirectly() {
        try {
            val db = AppDatabase.getDatabase(context)
            db.chatDao().clearHistory()
            db.learningHistoryDao().clearHistory()
            db.unifiedHistoryDao().deleteAllSessions()
            db.unifiedHistoryDao().deleteAllMessages()
            db.userProfileDao().clearProfile()
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "Error clearing local cache tables directly", e)
        }
    }

    fun clearLocalCacheOnly() {
        CoroutineScope(Dispatchers.IO).launch {
            clearLocalCacheDirectly()
        }
    }

    fun logout() {
        android.util.Log.i("NYAYA_CRASH_DEBUG", "SessionManager: logout triggered - clearing session preference cache")
        sharedPreferences.edit()
            .remove("logged_in_user")
            .remove("is_logged_in")
            .apply()

        // 1. Sign out Firebase Auth
        try {
            FirebaseAuth.getInstance().signOut()
            android.util.Log.i("NYAYA_CRASH_DEBUG", "SessionManager: Firebase Auth signed out successfully")
        } catch (e: Exception) {
            android.util.Log.e("NYAYA_CRASH_DEBUG", "SessionManager: Firebase Auth signout error", e)
        }

        // 2. Clear local SQLite cache only. Never delete Firestore data.
        clearLocalCacheOnly()
    }
}
