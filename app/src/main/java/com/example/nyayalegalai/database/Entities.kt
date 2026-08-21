package com.example.nyayalegalai.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0L,
    val title: String = "",
    val chatbotType: String = "", // "AI_ASSISTANT", "LEGAL_LEARNING", "CONSULTATION", "ENCYCLOPEDIA"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(tableName = "chat_history_messages")
data class ChatHistoryMessage(
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0L,
    val sessionId: Long = 0L,
    val sender: String = "", // "User" or "Bot"
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val language: String = "en",
    val learningProgress: Int = 0
)

@Entity(tableName = "learning_progress")
data class LearningProgress(
    @PrimaryKey val categoryId: String = "",
    val categoryName: String = "",
    val progress: Int = 0
)

@Entity(tableName = "learning_history")
data class LearningHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val question: String = "",
    val answer: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "law_entries")
data class LawEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val category: String = "",
    val sectionNumber: String = "",
    val description: String = "", // Simple Meaning
    val fullExplanation: String = "", // Detailed Explanation
    val punishment: String = "",
    val example: String = "", // Real-life Example
    val relatedLaws: String = "",
    val keywords: String = "", // Comma separated
    val importantPoints: String = "", // Bullet points
    val disclaimer: String = "This information is for educational purposes only and is not legal advice.",
    val bailable: String = "",
    val cognizable: String = "",
    val court: String = "",
    val notes: String = ""
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val email: String = "",
    val password: String = "",
    val name: String = "",
    val role: String = "user", // "user" or "lawyer"
    val phone: String = "",
    val barId: String? = null,
    val specialization: String? = null,
    val experience: String? = null,
    val location: String? = null,
    val bio: String? = null
)

@Entity(tableName = "consultation_requests")
data class ConsultationRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "",
    val lawyerId: String = "",
    val userName: String = "",
    val issueType: String = "",
    val description: String = "",
    val dateTime: String = "",
    val contactNumber: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val sessionId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return Gson().toJson(list)
    }
}
