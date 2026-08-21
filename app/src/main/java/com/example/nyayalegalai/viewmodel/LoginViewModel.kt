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

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                Log.d("FIRESTORE_DEBUG", "Starting Login Auth for $email...")
                val authResult = authRepo.login(email.trim(), pass)
                val firebaseUser = authResult.user ?: FirebaseAuth.getInstance().currentUser ?: throw Exception("Login failed: No Firebase user returned")
                val userId = firebaseUser.uid

                Log.d("FIRESTORE_DEBUG", "Login Auth SUCCESS, UID: $userId. Fetching user profile...")
                
                // Fetch profile with a timeout to avoid hangs
                val userProfile = try {
                    withTimeout(8000) {
                        firestoreRepo.getUserProfile(userId)
                    }
                } catch (e: Exception) {
                    Log.e("FIRESTORE_DEBUG", "User profile fetch error/timeout: ${e.message}")
                    null
                }

                var role = "USER"
                if (userProfile != null) {
                    role = if (userProfile.role.equals("CLIENT", ignoreCase = true) || userProfile.role.equals("user", ignoreCase = true) || userProfile.role.equals("USER", ignoreCase = true)) "USER" else userProfile.role.uppercase()
                    sessionManager.saveUser(LocalUser(userProfile.userId, userProfile.email, userProfile.name, role, userProfile.phone), pass)
                } else {
                    val lawyerProfile = try {
                        withTimeout(8000) {
                            firestoreRepo.getLawyerProfile(userId)
                        }
                    } catch (e: Exception) {
                        null
                    }

                    if (lawyerProfile != null) {
                        role = "LAWYER"
                        sessionManager.saveUser(LocalUser(lawyerProfile.lawyerId, lawyerProfile.email, lawyerProfile.name, "LAWYER", lawyerProfile.phone, lawyerProfile.barCouncilNumber), pass)
                    } else {
                        // First time profile creation if not exists
                        val defaultProfile = UserProfile(
                            userId = userId,
                            name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                            email = email.trim(),
                            role = "USER"
                        )
                        try {
                            firestoreRepo.saveUserProfile(defaultProfile)
                        } catch (e: Exception) {
                            Log.e("FIRESTORE_DEBUG", "Error saving default profile", e)
                        }
                        sessionManager.saveUser(LocalUser(userId, email.trim(), defaultProfile.name, "USER", ""), pass)
                    }
                }

                // Restore all existing user data from Firestore for this UID
                restoreUserData(userId)

                _loginState.value = LoginState.Success(role)
            } catch (e: Exception) {
                Log.e("FIRESTORE_DEBUG", "Login FLOW FAILED", e)
                _loginState.value = LoginState.Error(e.localizedMessage ?: "Invalid credentials or network issue")
            }
        }
    }

    suspend fun restoreUserData(userId: String) {
        Log.d("FIRESTORE_DEBUG", "Restoring complete user data from Firestore users/$userId")
        try {
            // 1. Clear local SQLite user data cache tables to guarantee complete account separation
            db.chatDao().clearHistory()
            db.learningHistoryDao().clearHistory()
            db.unifiedHistoryDao().deleteAllSessions()
            db.unifiedHistoryDao().deleteAllMessages()
            db.userProfileDao().clearProfile()

            // 2. Restore profile to SQLite
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

            // 3. Restore settings
            val settings = firestoreRepo.getUserSettings(userId)
            if (settings != null) {
                sessionManager.setDarkMode(settings.darkMode)
                sessionManager.setThemeColor(settings.themeColor)
                sessionManager.setFontColor(settings.fontColor)
                settings.language?.let { sessionManager.setLanguage(it) }
            }

            // 4. Restore chat sessions & messages from users/{uid}/chatSessions
            val chatSessions = firestoreRepo.getRoomChatSessionsList(userId)
            Log.d("FIRESTORE_DEBUG", "Restoring ${chatSessions.size} chat sessions from Firestore for UID: $userId")
            for (session in chatSessions) {
                db.unifiedHistoryDao().insertSession(session)
                val messages = firestoreRepo.getRoomChatMessagesList(userId, session.sessionId)
                Log.d("FIRESTORE_DEBUG", "Restoring ${messages.size} messages for session ${session.sessionId}")
                for (msg in messages) {
                    db.unifiedHistoryDao().insertMessage(msg)
                }
            }

            // 5. Restore learning history list from users/{uid}/learningHistory
            val learningList = firestoreRepo.getLearningHistoryList(userId)
            Log.d("FIRESTORE_DEBUG", "Restoring ${learningList.size} learning history items from Firestore for UID: $userId")
            for (item in learningList) {
                db.learningHistoryDao().insertHistory(item)
            }

            Log.d("FIRESTORE_DEBUG", "All user data restored successfully for UID: $userId.")
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Failed to restore user data: ${e.message}", e)
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
