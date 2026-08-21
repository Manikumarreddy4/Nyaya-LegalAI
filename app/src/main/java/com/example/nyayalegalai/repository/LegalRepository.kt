package com.example.nyayalegalai.repository

import com.example.nyayalegalai.database.*
import kotlinx.coroutines.flow.Flow

class LegalRepository(private val db: AppDatabase) {
    val chatDao = db.chatDao()
    val userProfileDao = db.userProfileDao()
    val learningDao = db.learningDao()

    fun getAllMessages(): Flow<List<ChatMessage>> = chatDao.getAllMessages()
    suspend fun insertMessage(message: ChatMessage) = chatDao.insertMessage(message)
    suspend fun clearChat() = chatDao.clearHistory()

    fun getUserProfile(): Flow<UserProfile?> = userProfileDao.getUserProfile()
    suspend fun updateProfile(profile: UserProfile) = userProfileDao.updateProfile(profile)

    fun getAllProgress(): Flow<List<LearningProgress>> = learningDao.getAllProgress()
    suspend fun updateProgress(progress: LearningProgress) = learningDao.updateProgress(progress)
}
