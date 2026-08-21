package com.example.nyayalegalai.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nyayalegalai.models.UserProfile
import com.example.nyayalegalai.repository.FirestoreRepository
import com.example.nyayalegalai.repository.StorageRepository
import com.example.nyayalegalai.utils.LocaleHelper
import com.example.nyayalegalai.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val sessionManager: SessionManager,
    private val firestoreRepo: FirestoreRepository,
    private val storageRepo: StorageRepository
) : ViewModel() {
    
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userRole = MutableStateFlow("")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _profilePhotoUrl = MutableStateFlow("")
    val profilePhotoUrl: StateFlow<String> = _profilePhotoUrl.asStateFlow()

    init {
        loadUserInfo()
    }

    fun loadUserInfo() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid
        val localUser = sessionManager.getUser()
        if (!uid.isNullOrBlank()) {
            _userEmail.value = localUser?.email ?: FirebaseAuth.getInstance().currentUser?.email ?: ""
            _userName.value = localUser?.name ?: "User"
            val rawRole = localUser?.role ?: "USER"
            _userRole.value = if (rawRole.equals("lawyer", ignoreCase = true) || rawRole.equals("LAWYER", ignoreCase = true)) "LAWYER" else "USER"
            _profilePhotoUrl.value = sessionManager.getUserProfilePhoto(uid)

            viewModelScope.launch {
                try {
                    val profile = firestoreRepo.getUserProfile(uid)
                    _userProfile.value = profile
                    profile?.let {
                        _userName.value = it.name
                        val firestoreRole = if (it.role.equals("lawyer", ignoreCase = true) || it.role.equals("LAWYER", ignoreCase = true)) "LAWYER" else "USER"
                        _userRole.value = firestoreRole
                        if (it.email.isNotBlank()) _userEmail.value = it.email
                        if (it.profilePhotoUrl.isNotBlank()) {
                            _profilePhotoUrl.value = it.profilePhotoUrl
                            sessionManager.setUserProfilePhoto(uid, it.profilePhotoUrl)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Error loading profile for UID: $uid", e)
                }
            }
        } else {
            _userProfile.value = null
            _userName.value = ""
            _userRole.value = ""
            _userEmail.value = ""
            _profilePhotoUrl.value = ""
        }
    }

    fun updateProfile(name: String, role: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid ?: return
        val normalizedRole = if (role.equals("Lawyer", ignoreCase = true) || role.equals("LAWYER", ignoreCase = true)) "LAWYER" else "USER"
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "name" to name,
                    "role" to normalizedRole
                )
                firestoreRepo.updateUserProfile(uid, updates)
                val currentLocal = sessionManager.getUser()
                if (currentLocal != null) {
                    sessionManager.saveUser(currentLocal.copy(name = name, role = normalizedRole))
                }
                loadUserInfo()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating profile for UID: $uid", e)
            }
        }
    }

    fun uploadProfilePhoto(uri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid ?: return
        val localUriString = uri.toString()
        _profilePhotoUrl.value = localUriString
        sessionManager.setUserProfilePhoto(uid, localUriString)

        viewModelScope.launch {
            try {
                var remoteUrl = localUriString
                try {
                    remoteUrl = storageRepo.uploadFile("users/$uid/profile/profile_photo.jpg", uri)
                    _profilePhotoUrl.value = remoteUrl
                    sessionManager.setUserProfilePhoto(uid, remoteUrl)
                } catch (storageEx: Exception) {
                    Log.w("ProfileViewModel", "Firebase storage upload failed or not configured, using local URI reference", storageEx)
                }

                firestoreRepo.updateUserProfile(uid, mapOf("profilePhotoUrl" to remoteUrl))
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error saving profile photo for UID: $uid", e)
            }
        }
    }

    private val _language = MutableStateFlow("English")
    val language: StateFlow<String> = _language.asStateFlow()

    fun loadSelectedLanguage(context: Context) {
        val code = sessionManager.getLanguage() ?: "en"
        val langName = when (code) {
            "hi" -> "Hindi"
            "ta" -> "Tamil"
            "te" -> "Telugu"
            "ml" -> "Malayalam"
            "kn" -> "Kannada"
            else -> "English"
        }
        _language.value = langName
        LocaleHelper.setLocale(context, code)
    }

    fun updateLanguage(context: Context, lang: String, code: String) {
        _language.value = lang
        sessionManager.setLanguage(code)
        LocaleHelper.setLocale(context, code)
    }
}
