package com.example.nyayalegalai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nyayalegalai.database.AppDatabase
import com.example.nyayalegalai.repository.*
import com.example.nyayalegalai.utils.SessionManager

class ViewModelFactory(
    private val db: AppDatabase,
    private val sessionManager: SessionManager,
    private val context: Context
) : ViewModelProvider.Factory {

    private val encyclopediaRepo = EncyclopediaRepository(context)
    private val localLegalRepository = LocalLegalRepository(db.lawDao())
    private val authRepo = FirebaseAuthRepository()
    private val firestoreRepo = FirestoreRepository()
    private val storageRepo = StorageRepository()
    private val chatHistoryRepository = ChatHistoryRepository(db.unifiedHistoryDao(), sessionManager, firestoreRepo)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                ChatViewModel(firestoreRepo, sessionManager, chatHistoryRepository) as T
            }
            modelClass.isAssignableFrom(LawyerViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                LawyerViewModel(firestoreRepo) as T
            }
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                LoginViewModel(authRepo, firestoreRepo, sessionManager, db) as T
            }
            modelClass.isAssignableFrom(SignupViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                SignupViewModel(authRepo, firestoreRepo, sessionManager) as T
            }
            modelClass.isAssignableFrom(ConsultationViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                ConsultationViewModel(firestoreRepo, storageRepo, sessionManager) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                ProfileViewModel(sessionManager, firestoreRepo, storageRepo) as T
            }
            modelClass.isAssignableFrom(LegalLearningViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                LegalLearningViewModel(db, localLegalRepository, chatHistoryRepository, sessionManager, firestoreRepo, context) as T
            }
            modelClass.isAssignableFrom(LawViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                LawViewModel(encyclopediaRepo, chatHistoryRepository) as T
            }
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                AuthViewModel(db.userAccountDao(), sessionManager) as T
            }
            modelClass.isAssignableFrom(ThemeViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                ThemeViewModel(sessionManager, firestoreRepo) as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                HistoryViewModel(chatHistoryRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
