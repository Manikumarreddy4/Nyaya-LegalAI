package com.example.nyayalegalai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nyayalegalai.GroqAssistantManager
import com.example.nyayalegalai.models.*
import com.example.nyayalegalai.repository.ChatHistoryRepository
import com.example.nyayalegalai.repository.FirestoreRepository
import com.example.nyayalegalai.utils.SessionManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class ChatViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val sessionManager: SessionManager,
    private val chatHistoryRepository: ChatHistoryRepository
) : ViewModel() {

    private val TAG = "CHAT_DEBUG"

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    val sessions: StateFlow<List<FirestoreChatSession>> = chatHistoryRepository.getSessionsByType("AI_ASSISTANT")
        .map { list ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid ?: ""
            list.map {
                FirestoreChatSession(
                    sessionId = it.sessionId.toString(),
                    userId = uid,
                    chatbotType = it.chatbotType,
                    title = it.title,
                    updatedAt = Timestamp(Date(it.updatedAt)),
                    isPinned = it.isPinned
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<FirestoreChatMessage>> = _currentSessionId.flatMapLatest { sessionIdStr ->
        if (sessionIdStr.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            val sessionIdLong = sessionIdStr.toLongOrNull()
            if (sessionIdLong != null) {
                chatHistoryRepository.getMessagesForSession(sessionIdLong).map { list ->
                    list.map {
                        FirestoreChatMessage(
                            messageId = it.messageId.toString(),
                            sessionId = it.sessionId.toString(),
                            sender = it.sender,
                            message = it.message,
                            timestamp = Timestamp(Date(it.timestamp))
                        )
                    }
                }
            } else {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid ?: ""
                firestoreRepo.getChatMessages(sessionIdStr, uid)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSession(sessionId: String) {
        Log.d(TAG, "setSession: $sessionId")
        if (_currentSessionId.value != sessionId) {
            Log.d(TAG, "Session switched: $sessionId")
            _currentSessionId.value = sessionId
        }
    }

    fun startNewChat() {
        Log.d(TAG, "startNewChat: Starting new chat session")
        _currentSessionId.value = null
    }

    fun sendMessage(text: String, type: String = "AI_ASSISTANT") {
        val trimmedText = text.trim()
        if (trimmedText.isBlank() || _isSending.value) return

        viewModelScope.launch {
            try {
                _isSending.value = true
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid ?: run {
                    _errorMessage.value = "Authentication error: User not logged in."
                    return@launch
                }

                var sessionIdStr = _currentSessionId.value
                val sessionIdLong: Long

                if (sessionIdStr.isNullOrBlank()) {
                    val title = if (trimmedText.length > 30) trimmedText.take(27) + "..." else trimmedText
                    sessionIdLong = chatHistoryRepository.createSession(title, type)
                    _currentSessionId.value = sessionIdLong.toString()
                } else {
                    sessionIdLong = sessionIdStr.toLongOrNull() ?: chatHistoryRepository.createSession(trimmedText.take(30), type)
                    _currentSessionId.value = sessionIdLong.toString()
                }

                // 1. Save User Message
                chatHistoryRepository.addMessage(sessionIdLong, "User", trimmedText)

                // 2. Prepare Prompt
                val prompt = """
                    You are an Indian AI Legal Problem Assistant.
                    The user will describe a legal situation.
                    Do NOT simply explain legal sections.
                    Instead:
                    • Understand the situation.
                    • Identify the legal issue.
                    • Mention applicable Indian laws when relevant.
                    • Explain user rights.
                    • Explain possible legal remedies.
                    • Suggest practical next steps.
                    • Recommend consulting a qualified lawyer when necessary.
                    • Never fabricate legal facts.
                    • Clearly state when information is uncertain.

                    User Scenario: $trimmedText

                    You MUST respond in this EXACT format. Ensure there is a double newline before every main section:

                    📌 Summary of the Issue
                    [Provide a summary of the described legal problem/situation]

                    ⚖ Possible Applicable Indian Laws
                    [List the applicable Indian laws or sections relevant to this scenario]

                    🛡 Rights of the User
                    [Explain the user's legal rights in this situation]

                    👥 Responsibilities of the Other Party
                    [Explain the responsibilities of the other party involved]

                    📝 Suggested Next Legal Steps
                    [Outline recommended legal steps or remedies]

                    👮 Police or Lawyer Recommendation
                    [Advise on whether contacting the police or a lawyer is appropriate]

                    ⚠️ Disclaimer
                    This information is for informational purposes only and is not legal advice. Please consult a qualified lawyer for your specific case.

                    STRICT RULES:
                    - Do NOT simply explain legal sections.
                    - Never ask the user to enter section numbers.
                    - Keep your entire response within approximately 400 words.
                """.trimIndent()

                // 3. Call Groq
                val responseText = withContext(Dispatchers.IO) {
                    GroqAssistantManager.askQuestion(prompt)
                }

                // 4. Save Bot Message
                chatHistoryRepository.addMessage(sessionIdLong, "Bot", responseText)

            } catch (e: Throwable) {
                Log.e(TAG, "Unexpected error: ${e.localizedMessage}")
                _errorMessage.value = e.localizedMessage ?: "Failed to send message"
            } finally {
                _isSending.value = false
            }
        }
    }

    fun deleteChat(sessionId: String) {
        viewModelScope.launch {
            try {
                val idLong = sessionId.toLongOrNull()
                if (idLong != null) {
                    chatHistoryRepository.deleteSession(idLong)
                } else {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid
                    firestoreRepo.deleteChatSession(sessionId, uid)
                }
                if (_currentSessionId.value == sessionId) {
                    _currentSessionId.value = null
                }
            } catch (e: Throwable) {
                _errorMessage.value = "Failed to delete chat: ${e.localizedMessage}"
            }
        }
    }
}
