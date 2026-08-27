package com.example.nyayalegalai.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessage::class,
        UserProfile::class,
        LearningProgress::class,
        LearningHistory::class,
        LawEntry::class,
        UserAccount::class,
        ConsultationRequest::class,
        ChatSession::class,
        ChatHistoryMessage::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun learningDao(): LearningDao
    abstract fun learningHistoryDao(): LearningHistoryDao
    abstract fun lawDao(): LawDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun unifiedHistoryDao(): UnifiedHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nyaya_ai_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
