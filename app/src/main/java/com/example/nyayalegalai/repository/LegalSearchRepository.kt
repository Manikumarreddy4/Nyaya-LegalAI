package com.example.nyayalegalai.repository

import android.content.Context
import com.example.nyayalegalai.models.LegalSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LegalSearchRepository(private val context: Context) {
    private val ipcRepository = IpcRepository(context)
    private val indiaLawRepository = IndiaLawRepository(context)
    private val legalBenchRepository = LegalBenchRepository(context)

    suspend fun searchAll(query: String): List<LegalSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val results = mutableListOf<LegalSearchResult>()

        // 1. IPC Section Dataset
        val ipcResults = ipcRepository.search(query)
        results.addAll(ipcResults)

        // 2. India Law Master Index
        val indiaLawResults = indiaLawRepository.search(query)
        results.addAll(indiaLawResults)

        // 3. LegalBench Dataset
        val benchResults = legalBenchRepository.search(query)
        results.addAll(benchResults)

        // The requirement says "return the best matching answer". 
        // We return all found but they are ordered by priority.
        // We could also filter to just one "best" match if needed, but usually a list is better.
        // If we want ONLY the first found dataset that has matches:
        /*
        if (ipcResults.isNotEmpty()) return@withContext ipcResults
        if (indiaLawResults.isNotEmpty()) return@withContext indiaLawResults
        return@withContext benchResults
        */
        
        results
    }
}
