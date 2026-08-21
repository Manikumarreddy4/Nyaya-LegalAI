package com.example.nyayalegalai.repository

import com.example.nyayalegalai.database.ChatHistoryMessage
import com.example.nyayalegalai.database.ChatSession
import com.example.nyayalegalai.database.UnifiedHistoryDao
import com.example.nyayalegalai.models.FirestoreChatMessage
import com.example.nyayalegalai.models.FirestoreChatSession
import com.example.nyayalegalai.utils.SessionManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import java.util.Date

class ChatHistoryRepository(
    private val unifiedHistoryDao: UnifiedHistoryDao,
    private val sessionManager: SessionManager,
    private val firestoreRepo: FirestoreRepository
) {

    val allSessions: Flow<List<ChatSession>> = unifiedHistoryDao.getAllSessions()

    fun getSessionsByType(type: String): Flow<List<ChatSession>> = unifiedHistoryDao.getSessionsByType(type)

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

    fun getLastMessageForSession(sessionId: Long): Flow<ChatHistoryMessage?> =
        unifiedHistoryDao.getLastMessageForSession(sessionId)

    suspend fun deleteSession(sessionId: Long) {
        val session = unifiedHistoryDao.getSessionById(sessionId)
        if (session != null) {
            unifiedHistoryDao.deleteSession(session)
            unifiedHistoryDao.deleteMessagesForSession(sessionId)
            
            // Sync delete to Firestore under users/{uid}/chatSessions/{sessionId}
            val uid = getCurrentUid()
            if (!uid.isNullOrBlank()) {
                try {
                    firestoreRepo.deleteRoomChatSession(uid, sessionId)
                } catch (e: Exception) {
                    android.util.Log.e("SYNC_DEBUG", "Error syncing delete to Firestore", e)
                }
            }
        }
    }

    suspend fun deleteAllHistory() {
        unifiedHistoryDao.deleteAllSessions()
        
        // Sync clear to Firestore
        val uid = getCurrentUid()
        if (!uid.isNullOrBlank()) {
            try {
                val list = firestoreRepo.getRoomChatSessionsList(uid)
                for (session in list) {
                    firestoreRepo.deleteRoomChatSession(uid, session.sessionId)
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

    fun searchHistory(query: String): Flow<List<ChatSession>> = unifiedHistoryDao.searchSessions(query)
}
