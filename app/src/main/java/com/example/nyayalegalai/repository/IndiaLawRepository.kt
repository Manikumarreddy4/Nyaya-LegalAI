package com.example.nyayalegalai.repository

import android.content.Context
import com.example.nyayalegalai.models.LegalSearchResult
import com.example.nyayalegalai.utils.CsvReader

class IndiaLawRepository(private val context: Context) {
    private var lawData: List<Map<String, String>> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        lawData = CsvReader.readCsv(context, "india_law_master_index_dataset.csv")
    }

    fun search(query: String): List<LegalSearchResult> {
        return lawData.filter { row ->
            row["number"]?.contains(query, ignoreCase = true) == true ||
            row["title"]?.contains(query, ignoreCase = true) == true ||
            row["description"]?.contains(query, ignoreCase = true) == true
        }.map { row ->
            LegalSearchResult(
                section = row["number"] ?: "",
                title = row["title"] ?: "",
                description = row["description"] ?: "",
                source = "India Law Master Index"
            )
        }
    }
}
