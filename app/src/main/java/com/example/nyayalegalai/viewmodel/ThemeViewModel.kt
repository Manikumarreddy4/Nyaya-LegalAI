package com.example.nyayalegalai.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nyayalegalai.repository.FirestoreRepository
import com.example.nyayalegalai.utils.SessionManager
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val sessionManager: SessionManager,
    private val firestoreRepo: FirestoreRepository
) : ViewModel() {
    private val _isDarkMode = mutableStateOf(sessionManager.isDarkMode())
    val isDarkMode: State<Boolean> = _isDarkMode

    private val _themeColor = mutableStateOf(sessionManager.getThemeColor())
    val themeColor: State<String> = _themeColor

    private val _fontColor = mutableStateOf(sessionManager.getFontColor())
    val fontColor: State<String> = _fontColor

    private fun saveSettingsToFirestore() {
        val user = sessionManager.getUser()
        if (user != null) {
            val settings = com.example.nyayalegalai.models.SavedSettings(
                darkMode = _isDarkMode.value,
                themeColor = _themeColor.value,
                fontColor = _fontColor.value,
                language = sessionManager.getLanguage() ?: "en"
            )
            viewModelScope.launch {
                try {
                    firestoreRepo.saveSettings(user.uid, settings)
                } catch (e: Exception) {
                    android.util.Log.e("ThemeViewModel", "Error saving settings to Firestore", e)
                }
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        sessionManager.setDarkMode(enabled)
        saveSettingsToFirestore()
    }

    fun setThemeColor(color: String) {
        _themeColor.value = color
        sessionManager.setThemeColor(color)
        saveSettingsToFirestore()
    }

    fun setFontColor(color: String) {
        _fontColor.value = color
        sessionManager.setFontColor(color)
        saveSettingsToFirestore()
    }
}
