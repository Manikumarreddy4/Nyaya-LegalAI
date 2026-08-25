package com.example.nyayalegalai.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM law_entries WHERE sectionNumber = :sectionNumber LIMIT 1")
    suspend fun getLawBySectionNumber(sectionNumber: String): LawEntry?

    @Query("SELECT * FROM law_entries WHERE sectionNumber LIKE '%' || :sectionPart || '%' LIMIT 1")
    suspend fun findBySectionPart(sectionPart: String): LawEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProfile(profile: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()
}

@Dao
interface LearningDao {
    @Query("SELECT * FROM learning_progress")
    fun getAllProgress(): Flow<List<LearningProgress>>

    @Query("SELECT * FROM law_entries WHERE sectionNumber = :sectionNumber LIMIT 1")
    suspend fun getLawBySectionNumber(sectionNumber: String): LawEntry?

    @Query("SELECT * FROM law_entries WHERE sectionNumber LIKE '%' || :sectionPart || '%' LIMIT 1")
    suspend fun findBySectionPart(sectionPart: String): LawEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProgress(progress: LearningProgress)
}

@Dao
interface LearningHistoryDao {
    @Query("SELECT * FROM learning_history ORDER BY timestamp DESC")
    fun getRecentHistory(): Flow<List<LearningHistory>>

    @Query("SELECT * FROM law_entries WHERE sectionNumber = :sectionNumber LIMIT 1")
    suspend fun getLawBySectionNumber(sectionNumber: String): LawEntry?

    @Query("SELECT * FROM law_entries WHERE sectionNumber LIKE '%' || :sectionPart || '%' LIMIT 1")
    suspend fun findBySectionPart(sectionPart: String): LawEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: LearningHistory): Long

    @Delete
    suspend fun deleteHistory(history: LearningHistory)

    @Query("SELECT * FROM learning_history")
    suspend fun getAllHistoryList(): List<LearningHistory>

    @Query("DELETE FROM learning_history")
    suspend fun clearHistory()
}

@Dao
interface LawDao {
    @Query("SELECT * FROM law_entries")
    fun getAllLaws(): Flow<List<LawEntry>>

    @Query("SELECT COUNT(*) FROM law_entries")
    suspend fun getLawCount(): Int

    @Query("SELECT * FROM law_entries WHERE id = :id")
    suspend fun getLawById(id: Int): LawEntry?

    @Query("SELECT * FROM law_entries WHERE title LIKE '%' || :query || '%' OR sectionNumber LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%'")
    fun searchLaws(query: String): Flow<List<LawEntry>>

    @Query("SELECT * FROM law_entries WHERE title LIKE '%' || :query || '%' OR sectionNumber LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%' LIMIT 10")
    suspend fun searchLawsSuspend(query: String): List<LawEntry>

    @Query("SELECT * FROM law_entries LIMIT :limit")
    fun getAllLawsLimited(limit: Int): Flow<List<LawEntry>>

    @Query("SELECT * FROM law_entries WHERE category = :category")
    fun getLawsByCategory(category: String): Flow<List<LawEntry>>

    @Query("SELECT * FROM law_entries WHERE category = :category AND (title LIKE '%' || :query || '%' OR sectionNumber LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%')")
    fun searchLawsByCategory(query: String, category: String): Flow<List<LawEntry>>

    @Query("SELECT * FROM law_entries WHERE sectionNumber = :sectionNumber LIMIT 1")
    suspend fun getLawBySectionNumber(sectionNumber: String): LawEntry?

    @Query("SELECT * FROM law_entries WHERE sectionNumber LIKE '%' || :sectionPart || '%' LIMIT 1")
    suspend fun findBySectionPart(sectionPart: String): LawEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaws(laws: List<LawEntry>)

    @Query("DELETE FROM law_entries")
    suspend fun deleteAllLaws()
}

@Dao
interface UserAccountDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registerUser(user: UserAccount)

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE email = :email AND password = :password LIMIT 1")
    suspend fun login(email: String, password: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE role = 'lawyer'")
    fun getAllLawyers(): Flow<List<UserAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bookConsultation(request: ConsultationRequest)

    @Query("SELECT * FROM consultation_requests WHERE userId = :userId ORDER BY timestamp DESC")
    fun getMyBookings(userId: String): Flow<List<ConsultationRequest>>

    @Query("SELECT * FROM consultation_requests WHERE lawyerId = :lawyerId ORDER BY timestamp DESC")
    fun getLawyerRequests(lawyerId: String): Flow<List<ConsultationRequest>>

    @Query("SELECT * FROM consultation_requests WHERE id = :requestId")
    suspend fun getRequestById(requestId: Int): ConsultationRequest?

    @Query("UPDATE consultation_requests SET status = :status WHERE id = :requestId")
    suspend fun updateRequestStatus(requestId: Int, status: String)
}

@Dao
interface UnifiedHistoryDao {
    @Query("SELECT * FROM chat_sessions ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE chatbotType = :type ORDER BY isPinned DESC, updatedAt DESC")
    fun getSessionsByType(type: String): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :id")
    suspend fun getSessionById(id: Long): ChatSession?

    @Query("SELECT * FROM chat_sessions")
    suspend fun getAllSessionsList(): List<ChatSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Update
    suspend fun updateSession(session: ChatSession)

    @Delete
    suspend fun deleteSession(session: ChatSession)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT * FROM chat_history_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatHistoryMessage>>

    @Query("SELECT * FROM chat_history_messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    fun getLastMessageForSession(sessionId: Long): Flow<ChatHistoryMessage?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatHistoryMessage): Long

    @Query("DELETE FROM chat_history_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    @Query("DELETE FROM chat_history_messages")
    suspend fun deleteAllMessages()

    @Query("UPDATE chat_sessions SET title = :newTitle, updatedAt = :timestamp WHERE sessionId = :sessionId")
    suspend fun renameSession(sessionId: Long, newTitle: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE chat_sessions SET isPinned = :pinned WHERE sessionId = :sessionId")
    suspend fun setPinned(sessionId: Long, pinned: Boolean)

    @Query("SELECT * FROM chat_sessions WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchSessions(query: String): Flow<List<ChatSession>>
}
