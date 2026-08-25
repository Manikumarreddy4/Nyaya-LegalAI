package com.example.nyayalegalai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nyayalegalai.repository.FirebaseAuthRepository
import com.example.nyayalegalai.repository.FirestoreRepository
import com.example.nyayalegalai.utils.SessionManager
import com.example.nyayalegalai.utils.LocalUser
import com.example.nyayalegalai.database.AppDatabase
import com.example.nyayalegalai.models.UserProfile
import com.example.nyayalegalai.models.SavedSettings
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class LoginViewModel(
    private val authRepo: FirebaseAuthRepository,
    private val firestoreRepo: FirestoreRepository,
    private val sessionManager: SessionManager,
    private val db: AppDatabase
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _loginState.value = LoginState.Error("Email and password cannot be empty")
            return
        }
        if (_loginState.value is LoginState.Loading) {
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                Log.d("LOGIN_PERF", "Login Auth started for $email")
                val authResult = authRepo.login(email.trim(), pass)
                val firebaseUser = authResult.user ?: FirebaseAuth.getInstance().currentUser ?: throw Exception("Login failed: No Firebase user returned")
                val userId = firebaseUser.uid

                // Step 1: Check cached accounts
                val savedAccount = sessionManager.getSavedAccounts().find { it.uid == userId || it.email.equals(email.trim(), ignoreCase = true) }
                var role = savedAccount?.role?.uppercase()

                if (savedAccount != null && role != null) {
                    sessionManager.saveUser(LocalUser(userId, email.trim(), savedAccount.name, role, ""), pass)
                    _loginState.value = LoginState.Success(role)
                    return@launch
                }

                // Step 2: Fetch profile from Firestore
                var userProfile: UserProfile? = null
                try {
                    withTimeout(4000) { userProfile = firestoreRepo.getUserProfile(userId) }
                } catch (e: Exception) {
                    Log.w("LOGIN_PERF", "UserProfile fetch timeout: ${e.message}")
                }
                
                var finalRole = "USER"
                if (userProfile != null) {
                    finalRole = if (userProfile!!.role.equals("CLIENT", ignoreCase = true) || userProfile!!.role.equals("user", ignoreCase = true) || userProfile!!.role.equals("USER", ignoreCase = true)) "USER" else userProfile!!.role.uppercase()
                    sessionManager.saveUser(LocalUser(userId, userProfile!!.email, userProfile!!.name, finalRole, userProfile!!.phone), pass)
                } else {
                    var lawyerProfile: com.example.nyayalegalai.models.LawyerProfile? = null
                    try {
                        withTimeout(4000) { lawyerProfile = firestoreRepo.getLawyerProfile(userId) }
                    } catch (e: Exception) {
                        Log.w("LOGIN_PERF", "LawyerProfile fetch timeout")
                    }

                    if (lawyerProfile != null) {
                        finalRole = "LAWYER"
                        sessionManager.saveUser(LocalUser(userId, lawyerProfile!!.email, lawyerProfile!!.name, "LAWYER", lawyerProfile!!.phone, lawyerProfile!!.barCouncilNumber), pass)
                    } else {
                        val defaultProfile = UserProfile(userId = userId, name = email.substringBefore("@").replaceFirstChar { it.uppercase() }, email = email.trim(), role = "USER")
                        try { firestoreRepo.saveUserProfile(defaultProfile) } catch (e: Exception) { Log.e("LOGIN_PERF", "Default profile save error", e) }
                        sessionManager.saveUser(LocalUser(userId, email.trim(), defaultProfile.name, "USER", ""), pass)
                    }
                }

                _loginState.value = LoginState.Success(finalRole)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("FIRESTORE_DEBUG", "Login FLOW FAILED", e)
                _loginState.value = LoginState.Error(e.localizedMessage ?: "Invalid credentials or network issue")
            }
        }
    }

    suspend fun restoreUserData(userId: String) {
        Log.d("FIRESTORE_DEBUG", "Restoring complete user data from Firestore users/$userId")
        try {
            // 1. Clear local SQLite user data cache
            try {
                db.chatDao().clearHistory()
                db.learningHistoryDao().clearHistory()
                db.unifiedHistoryDao().deleteAllSessions()
                db.unifiedHistoryDao().deleteAllMessages()
                db.userProfileDao().clearProfile()
            } catch (e: Exception) { Log.e("FIRESTORE_DEBUG", "Clear local DB failed", e) }

            // 2. Restore profile to SQLite
            try {
                val profile = firestoreRepo.getUserProfile(userId)
                if (profile != null) {
                    db.userProfileDao().updateProfile(
                        com.example.nyayalegalai.database.UserProfile(
                            id = 1,
                            name = profile.name,
                            language = sessionManager.getLanguage() ?: "en",
                            learningProgress = 0
                        )
                    )
                }
            } catch (e: Exception) { Log.e("FIRESTORE_DEBUG", "Restore profile failed", e) }

            // 3. Restore settings
            try {
                val settings = firestoreRepo.getUserSettings(userId)
                if (settings != null) {
                    sessionManager.setDarkMode(settings.darkMode)
                    sessionManager.setThemeColor(settings.themeColor)
                    sessionManager.setFontColor(settings.fontColor)
                    settings.language?.let { sessionManager.setLanguage(it) }
                }
            } catch (e: Exception) { Log.e("FIRESTORE_DEBUG", "Restore settings failed", e) }

            // 4. Restore chat sessions & messages
            try {
                val chatSessions = firestoreRepo.getRoomChatSessionsList(userId)
                for (session in chatSessions) {
                    db.unifiedHistoryDao().insertSession(session)
                    val messages = firestoreRepo.getRoomChatMessagesList(userId, session.sessionId)
                    for (msg in messages) {
                        db.unifiedHistoryDao().insertMessage(msg)
                    }
                }
            } catch (e: Exception) { Log.e("FIRESTORE_DEBUG", "Restore chat history failed", e) }

            // 5. Restore learning history
            try {
                val learningList = firestoreRepo.getLearningHistoryList(userId)
                for (item in learningList) {
                    db.learningHistoryDao().insertHistory(item)
                }
            } catch (e: Exception) { 
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("FIRESTORE_DEBUG", "Restore learning history failed", e) 
            }

            Log.d("FIRESTORE_DEBUG", "User data restored for UID: $userId.")
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FIRESTORE_DEBUG", "General failure in restoreUserData", e)
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _loginState.value = LoginState.Error("Please enter your email")
            return
        }
        viewModelScope.launch {
            try {
                authRepo.sendPasswordResetEmail(email.trim())
                _loginState.value = LoginState.Message("Password reset email sent")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _loginState.value = LoginState.Error(e.localizedMessage ?: "Failed to send reset email")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val role: String) : LoginState()
    data class Message(val msg: String) : LoginState()
    data class Error(val message: String) : LoginState()
}
