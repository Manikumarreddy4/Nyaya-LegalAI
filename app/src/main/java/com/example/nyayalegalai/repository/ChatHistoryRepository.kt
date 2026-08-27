package com.example.nyayalegalai.repository

import com.example.nyayalegalai.database.ChatHistoryMessage
import com.example.nyayalegalai.database.ChatSession
import com.example.nyayalegalai.database.UnifiedHistoryDao
import com.example.nyayalegalai.database.LearningHistoryDao
import com.example.nyayalegalai.models.FirestoreChatMessage
import com.example.nyayalegalai.models.FirestoreChatSession
import com.example.nyayalegalai.utils.SessionManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import java.util.Date
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import com.example.nyayalegalai.toRoomChatSession
import com.example.nyayalegalai.getSafeLong

data class ActivityStats(
    val chatCount: Int = 0,
    val learningCount: Int = 0,
    val totalCount: Int = 0
)

class ChatHistoryRepository(
    private val unifiedHistoryDao: UnifiedHistoryDao,
    private val sessionManager: SessionManager,
    private val firestoreRepo: FirestoreRepository,
    private val learningHistoryDao: LearningHistoryDao
) {

    val allSessions: Flow<List<ChatSession>> = combine(
        unifiedHistoryDao.getAllSessions(),
        learningHistoryDao.getRecentHistory()
    ) { chatSessions, learningList ->
        val filteredChats = chatSessions.filter { it.chatbotType != "ENCYCLOPEDIA" && it.chatbotType != "LEGAL_LEARNING" }
        val mappedLearnings = learningList.map { item ->
            ChatSession(
                sessionId = item.id.hashCode().toLong(),
                title = item.question,
                chatbotType = "LEGAL_LEARNING",
                createdAt = item.timestamp,
                updatedAt = item.timestamp
            )
        }
        (filteredChats + mappedLearnings).sortedByDescending { it.updatedAt }
    }

    fun getActivityStatsFlow(uid: String): Flow<ActivityStats> {
        if (uid.isBlank()) return flowOf(ActivityStats())
        
        val chatSessionsFlow = unifiedHistoryDao.getAllSessions()
        val learningHistoryFlow = learningHistoryDao.getRecentHistory()
        
        return combine(chatSessionsFlow, learningHistoryFlow) { chatSessions, learningList ->
            android.util.Log.d("ACTIVITY_SYNC", "ACTIVITY_SYNC: Room database stats update received")
            android.util.Log.d("ACTIVITY_SYNC", "ACTIVITY_SYNC: Current user ID = $uid")

            val aiAssistantSessionIds = mutableSetOf<Long>()
            
            chatSessions.forEach { s ->
                if (s.chatbotType == "AI_ASSISTANT") {
                    val titleText = s.title.trim()
                    val isTitleEmptyOrDefault = titleText.isEmpty() || 
                        titleText.startsWith("New Legal Query") || 
                        titleText.startsWith("New Chat") || 
                        titleText.startsWith("Untitled")
                    if (!isTitleEmptyOrDefault) {
                        aiAssistantSessionIds.add(s.sessionId)
                    }
                }
            }
            
            val aiHelpCount = aiAssistantSessionIds.size
            
            val learningCount = learningList.count {
                val questionText = it.question.trim()
                questionText.isNotEmpty() && 
                    !questionText.startsWith("New Search") && 
                    !questionText.startsWith("Untitled")
            }
            
            val totalChats = aiHelpCount + learningCount
            android.util.Log.d("ACTIVITY_SYNC", "ACTIVITY_SYNC: AI Help count = $aiHelpCount")
            android.util.Log.d("ACTIVITY_SYNC", "ACTIVITY_SYNC: Legal Learning count = $learningCount")
            android.util.Log.d("ACTIVITY_SYNC", "ACTIVITY_SYNC: Total Chats = $totalChats")

            ActivityStats(
                chatCount = aiHelpCount,
                learningCount = learningCount,
                totalCount = totalChats
            )
        }
    }

    fun getSessionsByType(type: String): Flow<List<ChatSession>> {
        return if (type == "LEGAL_LEARNING") {
            learningHistoryDao.getRecentHistory().map { list ->
                list.map { item ->
                    ChatSession(
                        sessionId = item.id.hashCode().toLong(),
                        title = item.question,
                        chatbotType = "LEGAL_LEARNING",
                        createdAt = item.timestamp,
                        updatedAt = item.timestamp
                    )
                }
            }
        } else {
            unifiedHistoryDao.getSessionsByType(type)
        }
    }

    private fun getCurrentUid(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid ?: sessionManager.getUser()?.uid
    }

    suspend fun createSession(title: String, type: String): Long {
        val session = ChatSession(title = title, chatbotType = type)
        val localId = unifiedHistoryDao.insertSession(session)
        
        // Sync to Firestore under users/{uid}/chatSessions/{localId}
        val uid = getCurrentUid()
        if (!uid.isNullOrBlank()) {
            try {
                val finalSession = session.copy(sessionId = localId)
                firestoreRepo.saveRoomChatSession(uid, finalSession)
            } catch (e: Exception) {
                android.util.Log.e("SYNC_DEBUG", "Error syncing session create to Firestore", e)
            }
        }
        return localId
    }

    suspend fun getSessionById(id: Long): ChatSession? = unifiedHistoryDao.getSessionById(id)

    suspend fun addMessage(sessionId: Long, sender: String, message: String) {
        val chatMessage = ChatHistoryMessage(sessionId = sessionId, sender = sender, message = message)
        val localMsgId = unifiedHistoryDao.insertMessage(chatMessage)
        
        // Update session's updatedAt timestamp
        val session = unifiedHistoryDao.getSessionById(sessionId)
        if (session != null) {
            val updatedSession = session.copy(updatedAt = System.currentTimeMillis())
            unifiedHistoryDao.updateSession(updatedSession)
            
            // Sync session update and message to Firestore under users/{uid}/chatSessions
            val uid = getCurrentUid()
            if (!uid.isNullOrBlank()) {
                try {
                    firestoreRepo.saveRoomChatSession(uid, updatedSession)
                    val finalMsg = chatMessage.copy(messageId = localMsgId)
                    firestoreRepo.saveRoomChatMessage(uid, finalMsg)
                } catch (e: Exception) {
                    android.util.Log.e("SYNC_DEBUG", "Error syncing message to Firestore", e)
                }
            }
        }
    }

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatHistoryMessage>> =
        unifiedHistoryDao.getMessagesForSession(sessionId)

    fun getLastMessageForSession(sessionId: Long): Flow<ChatHistoryMessage?> {
        val chatFlow = unifiedHistoryDao.getLastMessageForSession(sessionId)
        val learningFlow = learningHistoryDao.getRecentHistory().map { list ->
            val matching = list.firstOrNull { it.id.hashCode().toLong() == sessionId }
            if (matching != null) {
                ChatHistoryMessage(
                    messageId = sessionId,
                    sessionId = sessionId,
                    sender = "Bot",
                    message = matching.answer,
                    timestamp = matching.timestamp
                )
            } else {
                null
            }
        }
        return combine(chatFlow, learningFlow) { chatMsg, learnMsg ->
            learnMsg ?: chatMsg
        }
    }

    suspend fun deleteSession(sessionId: Long) {
        val session = unifiedHistoryDao.getSessionById(sessionId)
        if (session != null) {
            val messages = unifiedHistoryDao.getMessagesForSessionList(sessionId)
            
            // Delete locally first (optimistic update)
            unifiedHistoryDao.deleteSession(session)
            unifiedHistoryDao.deleteMessagesForSession(sessionId)
            
            // If it's a LEGAL_LEARNING session, delete the corresponding local LearningHistory items optimistically
            val matchingItems = mutableListOf<com.example.nyayalegalai.database.LearningHistory>()
            if (session.chatbotType == "LEGAL_LEARNING") {
                val firstMsg = messages.firstOrNull { it.sender == "User" }
                if (firstMsg != null) {
                    val queryText = firstMsg.message.trim()
                    matchingItems.addAll(
                        learningHistoryDao.getAllHistoryList()
                            .filter { it.question.trim().equals(queryText, ignoreCase = true) }
                    )
                    for (item in matchingItems) {
                        learningHistoryDao.deleteHistory(item)
                    }
                }
            }
            
            try {
                val uid = getCurrentUid()
                if (!uid.isNullOrBlank()) {
                    firestoreRepo.deleteRoomChatSession(uid, sessionId)
                    for (item in matchingItems) {
                        firestoreRepo.deleteLearningHistoryItem(uid, item.id)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SYNC_DEBUG", "Error syncing delete to Firestore, rolling back local changes", e)
                // Rollback Room database to original state
                unifiedHistoryDao.insertSession(session)
                for (msg in messages) {
                    unifiedHistoryDao.insertMessage(msg)
                }
                for (item in matchingItems) {
                    learningHistoryDao.insertHistory(item)
                }
                throw e
            }
        } else {
            // Check if it's a virtual LEGAL_LEARNING session mapping to a LearningHistory item
            val allHistory = learningHistoryDao.getAllHistoryList()
            val matchingHistoryItem = allHistory.firstOrNull { it.id.hashCode().toLong() == sessionId }
            if (matchingHistoryItem != null) {
                // Delete locally first (optimistic update)
                learningHistoryDao.deleteHistory(matchingHistoryItem)
                
                try {
                    val uid = getCurrentUid()
                    if (!uid.isNullOrBlank()) {
                        firestoreRepo.deleteLearningHistoryItem(uid, matchingHistoryItem.id)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SYNC_DEBUG", "Error syncing virtual delete to Firestore, rolling back local changes", e)
                    // Rollback
                    learningHistoryDao.insertHistory(matchingHistoryItem)
                    throw e
                }
            }
        }
    }

    suspend fun deleteSessions(sessionIds: List<Long>) {
        sessionIds.forEach { deleteSession(it) }
    }

    suspend fun deleteAllHistory() {
        unifiedHistoryDao.deleteAllSessions()
        learningHistoryDao.clearHistory()
        
        // Sync clear to Firestore
        val uid = getCurrentUid()
        if (!uid.isNullOrBlank()) {
            try {
                val list = firestoreRepo.getRoomChatSessionsList(uid)
                for (session in list) {
                    firestoreRepo.deleteRoomChatSession(uid, session.sessionId)
                }
                val learningList = firestoreRepo.getLearningHistoryList(uid)
                for (item in learningList) {
                    firestoreRepo.deleteLearningHistoryItem(uid, item.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("SYNC_DEBUG", "Error syncing clear to Firestore", e)
            }
        }
    }

    suspend fun renameSession(sessionId: Long, newTitle: String) {
        val timestamp = System.currentTimeMillis()
        unifiedHistoryDao.renameSession(sessionId, newTitle, timestamp)
        
        // Sync rename to Firestore
        val uid = getCurrentUid()
        if (!uid.isNullOrBlank()) {
            try {
                firestoreRepo.renameRoomChatSession(uid, sessionId, newTitle)
            } catch (e: Exception) {
                android.util.Log.e("SYNC_DEBUG", "Error syncing rename to Firestore", e)
            }
        }
    }

    suspend fun togglePin(sessionId: Long, pinned: Boolean) {
        unifiedHistoryDao.setPinned(sessionId, pinned)
        
        // Sync pin to Firestore
        val uid = getCurrentUid()
        if (!uid.isNullOrBlank()) {
            try {
                firestoreRepo.pinRoomChatSession(uid, sessionId, pinned)
            } catch (e: Exception) {
                android.util.Log.e("SYNC_DEBUG", "Error syncing pin to Firestore", e)
            }
        }
    }

    fun searchHistory(query: String): Flow<List<ChatSession>> = unifiedHistoryDao.searchSessions(query).map { list ->
        list.filter { it.chatbotType != "ENCYCLOPEDIA" }
    }

    suspend fun refreshHistoryFromServer(uid: String) {
        if (uid.isBlank()) return
        try {
            val dbInstance = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            
            // 1. Fetch chatSessions from server
            val chatSnapshot = dbInstance.collection("users").document(uid).collection("chatSessions")
                .get(Source.SERVER).await()
            val firestoreSessionIds = mutableSetOf<Long>()
            for (doc in chatSnapshot.documents) {
                val session = doc.toRoomChatSession()
                if (session != null) {
                    firestoreSessionIds.add(session.sessionId)
                    unifiedHistoryDao.insertSession(session)
                    
                    val messages = firestoreRepo.getRoomChatMessagesList(uid, session.sessionId)
                    for (msg in messages) {
                        unifiedHistoryDao.insertMessage(msg)
                    }
                }
            }
            
            val localSessions = unifiedHistoryDao.getAllSessionsList()
            for (localSess in localSessions) {
                if (localSess.sessionId !in firestoreSessionIds) {
                    unifiedHistoryDao.deleteSession(localSess)
                    unifiedHistoryDao.deleteMessagesForSession(localSess.sessionId)
                }
            }
            
            // 2. Fetch learningHistory from server
            val learnSnapshot = dbInstance.collection("users").document(uid).collection("learningHistory")
                .get(Source.SERVER).await()
            val firestoreHistoryIds = mutableSetOf<String>()
            for (doc in learnSnapshot.documents) {
                try {
                    val question = doc.getString("question") ?: doc.getString("query") ?: ""
                    val answer = doc.getString("answer") ?: doc.getString("explanation") ?: ""
                    val timestamp = doc.getSafeLong("timestamp", System.currentTimeMillis())
                    val id = doc.getSafeId("id")
                    
                    val item = com.example.nyayalegalai.database.LearningHistory(id = id, question = question, answer = answer, timestamp = timestamp)
                    firestoreHistoryIds.add(id)
                    learningHistoryDao.insertHistory(item)
                } catch (e: Exception) {
                    android.util.Log.e("SYNC_DEBUG", "Error parsing learning history document ${doc.id} during refresh", e)
                }
            }
            
            val localHistory = learningHistoryDao.getAllHistoryList()
            for (localItem in localHistory) {
                if (localItem.id !in firestoreHistoryIds) {
                    learningHistoryDao.deleteHistory(localItem)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC_DEBUG", "Error refreshing history from server", e)
        }
    }
}
