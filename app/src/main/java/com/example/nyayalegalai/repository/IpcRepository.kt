package com.example.nyayalegalai.repository

import android.content.Context
import com.example.nyayalegalai.models.LegalSearchResult
import com.example.nyayalegalai.utils.CsvReader

class IpcRepository(private val context: Context) {
    private var ipcData: List<Map<String, String>> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        ipcData = CsvReader.readCsv(context, "ipc_section_dataset_template.csv")
    }

    fun search(query: String): List<LegalSearchResult> {
        return ipcData.filter { row ->
            row["section"]?.contains(query, ignoreCase = true) == true ||
            row["title"]?.contains(query, ignoreCase = true) == true ||
            row["description"]?.contains(query, ignoreCase = true) == true ||
            row["keywords"]?.contains(query, ignoreCase = true) == true
        }.map { row ->
            LegalSearchResult(
                section = row["section"] ?: "",
                title = row["title"] ?: "",
                description = row["description"] ?: "",
                source = "IPC Section Dataset"
            )
        }
    }
}
