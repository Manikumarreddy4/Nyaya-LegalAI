package com.example.nyayalegalai.utils

import android.content.Context
import com.example.nyayalegalai.models.LegalScenario
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

object ScenarioMatcher {
    private var scenarios: List<LegalScenario> = emptyList()

    fun loadScenarios(context: Context) {
        try {
            context.assets.open("legal_scenarios.json").use { inputStream ->
                val reader = InputStreamReader(inputStream)
                val type = object : TypeToken<List<LegalScenario>>() {}.type
                scenarios = Gson().fromJson(reader, type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun findMatch(query: String): LegalScenario? {
        val lowercaseQuery = query.lowercase()
        // Find scenario with maximum keyword matches
        return scenarios.maxByOrNull { scenario ->
            scenario.keywords.count { keyword -> lowercaseQuery.contains(keyword.lowercase()) }
        }?.takeIf { scenario ->
            scenario.keywords.any { keyword -> lowercaseQuery.contains(keyword.lowercase()) }
        }
    }
}
