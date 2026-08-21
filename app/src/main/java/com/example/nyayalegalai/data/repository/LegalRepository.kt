package com.example.nyayalegalai.data.repository

import com.example.nyayalegalai.data.loader.AssetLoader
import org.json.JSONArray
import org.json.JSONObject

class LegalRepository(private val assetLoader: AssetLoader) {

    data class SearchResult(
        val id: String,
        val title: String,
        val content: String,
        val category: String
    )

    data class AnalysisResult(
        val situation: String,
        val possibleSections: List<String>,
        val possibleOffences: List<String>,
        val possiblePunishments: List<String>,
        val relatedArticles: List<String>,
        val suggestedNextSteps: List<String>,
        val disclaimer: String = "Educational Disclaimer: This analysis is generated from static datasets for educational purposes only. It does not constitute professional legal advice. Consult a qualified lawyer for legal matters."
    )

    fun searchIpcSection(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val data = assetLoader.loadJsonArray("ipc.json") ?: return results
        for (i in 0 until data.length()) {
            val obj = data.getJSONObject(i)
            val title = obj.optString("section_title")
            val desc = obj.optString("section_desc")
            val section = obj.optString("Section")
            if (title.contains(query, ignoreCase = true) || desc.contains(query, ignoreCase = true) || section.contains(query, ignoreCase = true)) {
                results.add(SearchResult(section, title, desc, "IPC"))
            }
        }
        return results
    }

    fun searchConstitutionArticle(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val data = assetLoader.loadJsonArray("constitution_of_india.json") ?: return results
        for (i in 0 until data.length()) {
            val obj = data.getJSONObject(i)
            val title = obj.optString("title")
            val desc = obj.optString("description")
            val article = obj.optString("article")
            if (title.contains(query, ignoreCase = true) || desc.contains(query, ignoreCase = true) || article.contains(query, ignoreCase = true)) {
                results.add(SearchResult(article, title, desc, "Constitution"))
            }
        }
        return results
    }

    fun searchBnsSection(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val data = assetLoader.loadJsonArray("bns_en.json") ?: return results
        for (i in 0 until data.length()) {
            val obj = data.getJSONObject(i)
            val title = obj.optString("title")
            val desc = obj.optString("description")
            val section = obj.optString("section")
            if (title.contains(query, ignoreCase = true) || desc.contains(query, ignoreCase = true) || section.contains(query, ignoreCase = true)) {
                results.add(SearchResult(section, title, desc, "BNS"))
            }
        }
        return results
    }

    fun searchActs(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        // User mentioned indian_acts.json, which might be missing but we try.
        // Also searching in MVA.json (Motor Vehicles Act)
        val files = listOf("indian_acts.json", "MVA.json")
        for (file in files) {
            val data = assetLoader.loadJsonArray(file) ?: continue
            for (i in 0 until data.length()) {
                val obj = data.getJSONObject(i)
                val title = obj.optString("title")
                val desc = obj.optString("description")
                val section = obj.optString("section")
                if (title.contains(query, ignoreCase = true) || desc.contains(query, ignoreCase = true)) {
                    results.add(SearchResult(section, title, desc, "Act"))
                }
            }
        }
        return results
    }

    fun searchWomenRights(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val data = assetLoader.loadJsonArray("women_rights.json") ?: return results
        for (i in 0 until data.length()) {
            val obj = data.getJSONObject(i)
            val title = obj.optString("title")
            val desc = obj.optString("description")
            if (title.contains(query, ignoreCase = true) || desc.contains(query, ignoreCase = true)) {
                results.add(SearchResult("WR", title, desc, "Women's Rights"))
            }
        }
        return results
    }

    fun searchEnvironmentalLaw(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val data = assetLoader.loadJsonArray("environmental_laws.json") ?: return results
        for (i in 0 until data.length()) {
            val obj = data.getJSONObject(i)
            val title = obj.optString("title")
            val desc = obj.optString("description")
            if (title.contains(query, ignoreCase = true) || desc.contains(query, ignoreCase = true)) {
                results.add(SearchResult("EL", title, desc, "Environmental Law"))
            }
        }
        return results
    }

    fun searchCyberLaw(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val data = assetLoader.loadJsonArray("cyber_laws.json") ?: return results
        for (i in 0 until data.length()) {
            val obj = data.getJSONObject(i)
            val title = obj.optString("title")
            val desc = obj.optString("description")
            if (title.contains(query, ignoreCase = true) || desc.contains(query, ignoreCase = true)) {
                results.add(SearchResult("CL", title, desc, "Cyber Law"))
            }
        }
        return results
    }

    fun getScenarioAnalysis(query: String): AnalysisResult {
        val sections = mutableListOf<String>()
        val offences = mutableListOf<String>()
        val punishments = mutableListOf<String>()
        val articles = mutableListOf<String>()
        val steps = mutableListOf<String>()

        val scenarios = assetLoader.loadJsonArray("legal_scenarios.json") ?: return AnalysisResult(query, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        
        var bestMatch: JSONObject? = null
        var maxMatches = 0

        val words = query.lowercase().split(" ", ",", ".", "?").filter { it.length > 3 }

        for (i in 0 until scenarios.length()) {
            val scenario = scenarios.getJSONObject(i)
            val keywordsArray = scenario.optJSONArray("keywords")
            var matchCount = 0
            if (keywordsArray != null) {
                for (j in 0 until keywordsArray.length()) {
                    if (words.contains(keywordsArray.getString(j).lowercase())) {
                        matchCount++
                    }
                }
            }
            if (matchCount > maxMatches) {
                maxMatches = matchCount
                bestMatch = scenario
            }
        }

        bestMatch?.let {
            offences.add(it.optString("issue"))
            sections.add(it.optString("relevant_laws"))
            steps.add(it.optString("guidance"))
            // Punishments and articles might be inferred or extracted from relevant_laws
        }

        return AnalysisResult(query, sections, offences, punishments, articles, steps)
    }

    fun getLegalLearningContent(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        // Search in ai_legal_learning.json
        assetLoader.loadJsonArray("ai_legal_learning.json")?.let { data ->
            for (i in 0 until data.length()) {
                val obj = data.getJSONObject(i)
                val title = obj.optString("title")
                val simpleDesc = obj.optString("simple_explanation")
                val detailedDesc = obj.optString("detailed_explanation")
                if (title.contains(query, ignoreCase = true) || simpleDesc.contains(query, ignoreCase = true) || detailedDesc.contains(query, ignoreCase = true)) {
                    results.add(SearchResult(obj.optString("id"), title, simpleDesc, "Learning"))
                }
            }
        }

        // Search in knowledge_base.json
        assetLoader.loadJsonObject("knowledge_base.json")?.optJSONArray("entries")?.let { entries ->
            for (i in 0 until entries.length()) {
                val obj = entries.getJSONObject(i)
                val title = obj.optString("title")
                val answer = obj.optString("answer")
                if (title.contains(query, ignoreCase = true) || answer.contains(query, ignoreCase = true)) {
                    results.add(SearchResult(obj.optString("id"), title, answer, obj.optString("category")))
                }
            }
        }

        // Search in IndicLegalQA Dataset_10K.json
        assetLoader.loadJsonArray("IndicLegalQA Dataset_10K.json")?.let { data ->
            for (i in 0 until data.length()) {
                val obj = data.getJSONObject(i)
                val question = obj.optString("question")
                val answer = obj.optString("answer")
                if (question.contains(query, ignoreCase = true) || answer.contains(query, ignoreCase = true)) {
                    results.add(SearchResult("QA_$i", question.take(50) + "...", answer, "Q&A"))
                }
            }
        }

        return results
    }
}
