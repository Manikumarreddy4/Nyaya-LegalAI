package com.example.nyayalegalai.repository

import android.content.Context
import com.example.nyayalegalai.models.LegalSearchResult
import com.example.nyayalegalai.utils.ZipReader

class LegalBenchRepository(private val context: Context) {
    private var benchData: List<Map<String, String>> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        benchData = ZipReader.readZipCsvFiles(context, "legalbench-main.zip")
    }

    fun search(query: String): List<LegalSearchResult> {
        return benchData.filter { row ->
            row.values.any { it.contains(query, ignoreCase = true) }
        }.map { row ->
            LegalSearchResult(
                section = row["index"] ?: row["id"] ?: "N/A",
                title = row["source_file"] ?: "LegalBench",
                description = row["text"] ?: row["question"] ?: row["answer"] ?: row.values.joinToString(" ").take(100) + "...",
                source = "LegalBench Dataset"
            )
        }.distinctBy { it.description }
    }
}
