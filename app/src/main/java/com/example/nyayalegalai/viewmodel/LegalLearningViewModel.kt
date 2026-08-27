package com.example.nyayalegalai.viewmodel

import com.example.nyayalegalai.GroqLearningManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nyayalegalai.database.AppDatabase
import com.example.nyayalegalai.database.ChatHistoryMessage
import com.example.nyayalegalai.database.LearningHistory
import com.example.nyayalegalai.repository.ChatHistoryRepository
import com.example.nyayalegalai.repository.FirestoreRepository
import com.example.nyayalegalai.repository.LocalLegalRepository
import com.example.nyayalegalai.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LegalLearningViewModel(
    private val db: AppDatabase,
    private val repository: LocalLegalRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val sessionManager: SessionManager,
    private val firestoreRepo: FirestoreRepository,
    context: Context
) : ViewModel() {

    private val TAG = "CHAT_DEBUG"

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            _currentSessionId.value = null
            _isLoading.value = false
        }
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val recentHistory: StateFlow<List<LearningHistory>> = db.learningHistoryDao().getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val messages: StateFlow<List<ChatHistoryMessage>> = _currentSessionId.flatMapLatest { sessionId ->
        if (sessionId == null) flowOf(emptyList())
        else chatHistoryRepository.getMessagesForSession(sessionId)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSession(sessionId: Long) {
        if (_currentSessionId.value != sessionId) {
            Log.d(TAG, "Session switched (Learning): $sessionId")
            _currentSessionId.value = sessionId
        }
    }

    fun startNewChat() {
        Log.d(TAG, "Starting new learning session")
        _currentSessionId.value = null
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun performAnalysis(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank() || _isLoading.value) return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                var sessionId = _currentSessionId.value
                if (sessionId == null) {
                    val title = if (trimmedQuery.length > 30) trimmedQuery.take(27) + "..." else trimmedQuery
                    sessionId = chatHistoryRepository.createSession(title, "LEGAL_LEARNING")
                    _currentSessionId.value = sessionId
                    Log.d(TAG, "Learning Session created: $sessionId")
                }

                // Save User Message
                chatHistoryRepository.addMessage(sessionId, "User", trimmedQuery)
                Log.d(TAG, "User message saved (Learning)")

                val prompt = """
                    You are an expert AI Legal Educator for Indian Law in the "Nyaya AI" application.
                    Answer the user's legal question in simple, clear English suitable for students.

                    User Query: $trimmedQuery

                    Respond strictly using the following structure:

                    📌 Law / Section Name

                    📖 Meaning

                    📜 Detailed Explanation

                    ⚖ Punishment (if applicable)

                    📝 Real-life Example (India)

                    🔗 Related Sections / Articles

                    💡 Important Notes
                """.trimIndent()

                Log.d(TAG, "Groq request started (Learning)")
                val result = try {
                    GroqLearningManager.askLearning(prompt)
                } catch (e: Exception) {
                    Log.e(TAG, "Groq call failed (Learning): ${e.message}")
                    "Groq Server Error."
                }
                Log.d(TAG, "Groq response received (Learning)")

                // Save Bot Message
                chatHistoryRepository.addMessage(sessionId, "Bot", result)
                Log.d(TAG, "Bot message saved (Learning)")

                // Save to History (local SQLite and remote Firestore)
                val docId = "learn_" + System.currentTimeMillis()
                val learningItem = LearningHistory(id = docId, question = trimmedQuery, answer = result, timestamp = System.currentTimeMillis())
                db.learningHistoryDao().insertHistory(learningItem)

                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid
                if (!uid.isNullOrBlank()) {
                    try {
                        firestoreRepo.saveLearningHistory(uid, learningItem)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        Log.e(TAG, "Error syncing learning history to Firestore", e)
                    }
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error in performAnalysis (Learning)", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteLearningHistory(item: LearningHistory) {
        viewModelScope.launch {
            // Local optimistic delete from Room
            db.learningHistoryDao().deleteHistory(item)
            
            // Find and delete matching ChatSession optimistically
            val queryText = item.question.trim()
            val localSessions = db.unifiedHistoryDao().getAllSessionsList()
            val deletedSessions = mutableListOf<com.example.nyayalegalai.database.ChatSession>()
            val deletedMessages = mutableListOf<List<com.example.nyayalegalai.database.ChatHistoryMessage>>()
            
            for (session in localSessions) {
                if (session.chatbotType == "LEGAL_LEARNING") {
                    val messages = db.unifiedHistoryDao().getMessagesForSessionList(session.sessionId)
                    val firstMsg = messages.firstOrNull { it.sender == "User" }
                    if (firstMsg != null && firstMsg.message.trim().equals(queryText, ignoreCase = true)) {
                        db.unifiedHistoryDao().deleteSession(session)
                        db.unifiedHistoryDao().deleteMessagesForSession(session.sessionId)
                        deletedSessions.add(session)
                        deletedMessages.add(messages)
                    }
                }
            }

            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid
                if (!uid.isNullOrBlank()) {
                    firestoreRepo.deleteLearningHistoryItem(uid, item.id)
                    for (session in deletedSessions) {
                        firestoreRepo.deleteRoomChatSession(uid, session.sessionId)
                    }
                }
                Log.d("HISTORY_DELETE", "HISTORY_DELETE: Delete successful")
            } catch (e: Exception) {
                Log.e("HISTORY_DELETE", "HISTORY_DELETE: Delete failed, rolling back local changes", e)
                // Rollback local Room changes
                db.learningHistoryDao().insertHistory(item)
                for (i in deletedSessions.indices) {
                    val session = deletedSessions[i]
                    db.unifiedHistoryDao().insertSession(session)
                    val messages = deletedMessages[i]
                    for (msg in messages) {
                        db.unifiedHistoryDao().insertMessage(msg)
                    }
                }
            }
        }
    }

    fun deleteLearningHistories(items: List<LearningHistory>) {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid
            val rollbackHistoryList = mutableListOf<LearningHistory>()
            val rollbackSessions = mutableListOf<com.example.nyayalegalai.database.ChatSession>()
            val rollbackMessages = mutableListOf<List<com.example.nyayalegalai.database.ChatHistoryMessage>>()

            // Local optimistic deletes
            items.forEach { item ->
                db.learningHistoryDao().deleteHistory(item)
                rollbackHistoryList.add(item)
                
                val queryText = item.question.trim()
                val localSessions = db.unifiedHistoryDao().getAllSessionsList()
                for (session in localSessions) {
                    if (session.chatbotType == "LEGAL_LEARNING") {
                        val messages = db.unifiedHistoryDao().getMessagesForSessionList(session.sessionId)
                        val firstMsg = messages.firstOrNull { it.sender == "User" }
                        if (firstMsg != null && firstMsg.message.trim().equals(queryText, ignoreCase = true)) {
                            db.unifiedHistoryDao().deleteSession(session)
                            db.unifiedHistoryDao().deleteMessagesForSession(session.sessionId)
                            rollbackSessions.add(session)
                            rollbackMessages.add(messages)
                        }
                    }
                }
            }

            try {
                if (!uid.isNullOrBlank()) {
                    items.forEach { item ->
                        firestoreRepo.deleteLearningHistoryItem(uid, item.id)
                    }
                    rollbackSessions.forEach { session ->
                        firestoreRepo.deleteRoomChatSession(uid, session.sessionId)
                    }
                }
                Log.d("HISTORY_DELETE", "HISTORY_DELETE: Bulk delete successful")
            } catch (e: Exception) {
                Log.e("HISTORY_DELETE", "HISTORY_DELETE: Bulk delete failed, rolling back local changes", e)
                // Rollback local Room changes
                rollbackHistoryList.forEach { item ->
                    db.learningHistoryDao().insertHistory(item)
                }
                for (i in rollbackSessions.indices) {
                    val session = rollbackSessions[i]
                    db.unifiedHistoryDao().insertSession(session)
                    val messages = rollbackMessages[i]
                    for (msg in messages) {
                        db.unifiedHistoryDao().insertMessage(msg)
                    }
                }
            }
        }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid
            if (!uid.isNullOrBlank()) {
                chatHistoryRepository.refreshHistoryFromServer(uid)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        } catch (e: Exception) {
            Log.e("LegalLearningViewModel", "Error removing authStateListener onCleared", e)
        }
    }
}
