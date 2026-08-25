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
import kotlinx.coroutines.tasks.await

class ProfileViewModel(
    private val sessionManager: SessionManager,
    private val firestoreRepo: FirestoreRepository,
    private val storageRepo: StorageRepository
) : ViewModel() {
    
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userPhone = MutableStateFlow("")
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    private val _userRole = MutableStateFlow("")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _profilePhotoUrl = MutableStateFlow("")
    val profilePhotoUrl: StateFlow<String> = _profilePhotoUrl.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            _userProfile.value = null
            _userName.value = ""
            _userPhone.value = ""
            _userRole.value = ""
            _userEmail.value = ""
            _profilePhotoUrl.value = ""
        } else {
            loadUserInfo()
        }
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    fun loadUserInfo() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: sessionManager.getUser()?.uid
        val localUser = sessionManager.getUser()
        if (!uid.isNullOrBlank()) {
            val authEmail = currentUser?.email ?: ""
            val authDisplayName = currentUser?.displayName ?: ""

            _userEmail.value = localUser?.email?.ifBlank { null } ?: authEmail
            _userName.value = localUser?.name?.ifBlank { null } ?: authDisplayName.ifBlank { "User" }
            _userPhone.value = localUser?.phone ?: ""
            val rawRole = localUser?.role ?: "USER"
            _userRole.value = if (rawRole.equals("lawyer", ignoreCase = true) || rawRole.equals("LAWYER", ignoreCase = true)) "LAWYER" else "USER"
            _profilePhotoUrl.value = sessionManager.getUserProfilePhoto(uid)

            viewModelScope.launch {
                try {
                    val profile = firestoreRepo.getUserProfile(uid)
                    _userProfile.value = profile
                    profile?.let {
                        _userName.value = it.name.ifBlank { authDisplayName.ifBlank { "User" } }
                        _userPhone.value = it.phone
                        val firestoreRole = if (it.role.equals("lawyer", ignoreCase = true) || it.role.equals("LAWYER", ignoreCase = true)) "LAWYER" else "USER"
                        _userRole.value = firestoreRole
                        _userEmail.value = it.email.ifBlank { authEmail }
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
            _userPhone.value = ""
            _userRole.value = ""
            _userEmail.value = ""
            _profilePhotoUrl.value = ""
        }
    }

    fun updateProfile(name: String, phone: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid ?: return
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "name" to name,
                    "phone" to phone
                )
                firestoreRepo.updateUserProfile(uid, updates)
                val currentLocal = sessionManager.getUser()
                if (currentLocal != null) {
                    sessionManager.saveUser(currentLocal.copy(name = name, phone = phone))
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

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: sessionManager.getUser()?.uid
        if (uid.isNullOrBlank()) {
            onError("User session not found")
            return
        }

        viewModelScope.launch {
            try {
                // Delete user document from Firestore users collection
                firestoreRepo.deleteUserProfile(uid)
                // Delete lawyer profile from Firestore lawyers collection (if exists)
                firestoreRepo.deleteLawyerProfile(uid)
                
                // Clear local session and cache
                sessionManager.logout()

                // Also delete from FirebaseAuth if signed in
                if (user != null) {
                    try {
                        user.delete().await()
                    } catch (authEx: Exception) {
                        Log.w("ProfileViewModel", "Failed to delete Firebase Auth user (could be already deleted)", authEx)
                    }
                }
                
                onSuccess()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error deleting account for UID $uid", e)
                try {
                    sessionManager.logout()
                } catch (ex: Exception) {}
                onError(e.localizedMessage ?: "Failed to delete account from server")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error removing authStateListener", e)
        }
    }
}
