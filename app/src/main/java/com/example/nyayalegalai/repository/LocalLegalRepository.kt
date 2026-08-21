package com.example.nyayalegalai.repository

import android.util.Log
import com.example.nyayalegalai.database.LawDao
import com.example.nyayalegalai.models.LegalSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalLegalRepository(private val lawDao: LawDao) {

    suspend fun searchLocal(query: String): List<LegalSearchResult> = withContext(Dispatchers.IO) {
        Log.d("LocalLegalRepository", "Searching database for: $query")
        try {
            val dbResults = lawDao.searchLawsSuspend(query)
            
            dbResults.map { entry ->
                LegalSearchResult(
                    section = entry.sectionNumber,
                    title = entry.title,
                    description = entry.description,
                    source = entry.category,
                    punishment = entry.punishment
                )
            }
        } catch (e: Exception) {
            Log.e("LocalLegalRepository", "Database search failed", e)
            emptyList()
        }
    }
}
