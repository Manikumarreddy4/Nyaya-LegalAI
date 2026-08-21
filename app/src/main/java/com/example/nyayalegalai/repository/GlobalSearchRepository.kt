package com.example.nyayalegalai.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

data class LawResult(
    val title: String,
    val sectionOrArticle: String,
    val description: String,
    val punishment: String,
    val relatedSections: List<String>,
    val sourceDataset: String,
    val type: String = "",
    val detailedExplanation: String = "",
    val practicalExample: String = "",
    val cognizable: String = "",
    val bailable: String = "",
    val court: String = "",
    val notes: String = "",
    val category: String = ""
)

class GlobalSearchRepository(private val context: Context) {
    private val gson = Gson()
    
    // In-memory cache for fast searching and full details
    private val allMergedLaws = mutableListOf<LawResult>()
    private val exactIndex = HashMap<String, LawResult>()
    
    private var isInitialized = false

    suspend fun initialize() {
        if (isInitialized) return
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            
            // 1. Load basic data from CSV into a temp map
            val mergedMap = loadIpcCsv()
            val csvCount = mergedMap.size
            Log.d("LegalData", "CSV records loaded: $csvCount")

            // 2. Load and Merge detailed data from JSONs
            val jsonCount = loadAndMergeJsonData(mergedMap)
            Log.d("LegalData", "JSON records processed: $jsonCount")

            allMergedLaws.clear()
            allMergedLaws.addAll(mergedMap.values)
            
            // 3. Build fast search index
            buildIndex()
            
            isInitialized = true
            Log.d("LegalData", "Merged records: ${allMergedLaws.size}")
            Log.d("NyayaAI", "GlobalSearchRepository initialized in ${System.currentTimeMillis() - startTime}ms")
        }
    }

    private fun loadIpcCsv(): MutableMap<String, LawResult> {
        val map = mutableMapOf<String, LawResult>()
        try {
            val inputStream = context.assets.open("ipc_sections_dataset.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // skip header
            
            var line: String? = reader.readLine()
            while (line != null) {
                val values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().removeSurrounding("\"") }
                if (values.size >= 4) {
                    val desc = values[0]
                    val offense = values[1]
                    val punishment = values[2]
                    val section = values[3]
                    
                    val law = LawResult(
                        title = offense,
                        sectionOrArticle = "Section $section",
                        description = desc.take(200),
                        punishment = punishment,
                        relatedSections = emptyList(),
                        sourceDataset = "ipc_sections_dataset.csv",
                        type = "IPC",
                        detailedExplanation = desc,
                        category = "Criminal Law"
                    )
                    val key = "IPC_" + normalizeSection(section)
                    map[key] = law
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("NyayaAI", "Error loading IPC CSV", e)
        }
        return map
    }

    private fun loadAndMergeJsonData(mergedMap: MutableMap<String, LawResult>): Int {
        var count = 0
        
        // 1. ai_legal_learning.json (Detailed IPC)
        val aiData = loadJsonArray("ai_legal_learning.json")
        for (item in aiData) {
            val id = item["id"]?.toString() ?: ""
            val type = if (id.startsWith("IPC")) "IPC" else if (id.startsWith("ARTICLE")) "Article" else ""
            if (type.isEmpty()) continue
            
            val section = id.replace("IPC_", "").replace("ARTICLE_", "")
            val key = type + "_" + normalizeSection(section)
            
            val detailedExp = item["detailed_explanation"]?.toString() ?: ""
            val punishment = item["punishment"]?.toString() ?: ""
            val example = item["real_life_example"]?.toString() ?: ""
            val bailable = item["bailable"]?.toString() ?: ""
            val title = item["title"]?.toString() ?: ""
            
            val existing = mergedMap[key]
            if (existing != null) {
                mergedMap[key] = existing.copy(
                    detailedExplanation = if (detailedExp.isNotEmpty()) detailedExp else existing.detailedExplanation,
                    punishment = if (punishment.isNotEmpty()) punishment else existing.punishment,
                    practicalExample = example,
                    bailable = bailable,
                    title = if (title.isNotEmpty()) title else existing.title
                )
            } else {
                mergedMap[key] = LawResult(
                    title = title,
                    sectionOrArticle = if (type == "Article") "Article $section" else "$type Section $section",
                    description = detailedExp.take(200),
                    punishment = punishment,
                    relatedSections = emptyList(),
                    sourceDataset = "ai_legal_learning.json",
                    type = type,
                    detailedExplanation = detailedExp,
                    practicalExample = example,
                    bailable = bailable
                )
            }
            count++
        }

        // 2. Process all other JSONs (BNS, Constitution, Cyber, etc.)
        val otherFiles = listOf(
            "bns_en.json", "bnss_en.json", "bsa_en.json", 
            "constitution_cleaned_dataset.json", "cpc.json", "crpc.json", "MVA.json",
            "cyber_laws.json", "women_rights.json", "environmental_laws.json"
        )
        
        for (file in otherFiles) {
            val data = loadJsonArray(file)
            for (item in data) {
                val section = item["section"]?.toString() ?: item["number"]?.toString() ?: item["article"]?.toString() ?: ""
                if (section.isEmpty()) continue
                
                val type = when {
                    file.contains("bns") -> "BNS"
                    file.contains("constitution") -> "Article"
                    file.contains("ipc") -> "IPC"
                    file.contains("cpc") -> "CPC"
                    file.contains("crpc") -> "CrPC"
                    file.contains("MVA") -> "MVA"
                    else -> ""
                }
                
                val key = if (type.isNotEmpty()) type + "_" + normalizeSection(section) else normalizeSection(section)
                
                // Avoid overwriting if we already have detailed IPC data
                if (type == "IPC" && mergedMap.containsKey(key) && mergedMap[key]?.sourceDataset == "ai_legal_learning.json") continue

                val title = item["offense"]?.toString() ?: item["title"]?.toString() ?: item["heading"]?.toString() ?: ""
                val desc = item["description"]?.toString() ?: item["definition"]?.toString() ?: item["explanation"]?.toString() ?: ""
                val punishment = item["punishment"]?.toString() ?: ""
                
                val law = LawResult(
                    title = title,
                    sectionOrArticle = if (type == "Article") "Article $section" else if (type.isNotEmpty()) "$type Section $section" else section,
                    description = desc.take(200),
                    punishment = punishment,
                    relatedSections = emptyList(),
                    sourceDataset = file,
                    type = type,
                    detailedExplanation = desc,
                    practicalExample = item["example"]?.toString() ?: item["practicalExample"]?.toString() ?: "",
                    cognizable = item["cognizable"]?.toString() ?: "",
                    bailable = item["bailable"]?.toString() ?: "",
                    court = item["court"]?.toString() ?: "",
                    category = item["category"]?.toString() ?: ""
                )
                
                val existing = mergedMap[key]
                if (existing != null) {
                    mergedMap[key] = existing.copy(
                        detailedExplanation = if (desc.length > existing.detailedExplanation.length) desc else existing.detailedExplanation,
                        punishment = if (punishment.isNotEmpty()) punishment else existing.punishment,
                        practicalExample = if (law.practicalExample.isNotEmpty()) law.practicalExample else existing.practicalExample
                    )
                } else {
                    mergedMap[key] = law
                }
                count++
            }
        }

        return count
    }

    private fun buildIndex() {
        exactIndex.clear()
        for (law in allMergedLaws) {
            val sectionNum = normalizeSection(law.sectionOrArticle)
            val type = law.type.lowercase()
            
            // Generate multiple searchable keys for this law
            val keys = mutableSetOf<String>()
            keys.add(normalize(type + sectionNum)) // e.g. "ipc302"
            keys.add(normalize(sectionNum)) // e.g. "302"
            keys.add(normalize("section" + sectionNum)) // e.g. "section302"
            keys.add(normalize(type + "section" + sectionNum)) // e.g. "ipcsection302"
            
            if (type == "article") {
                keys.add(normalize("article" + sectionNum)) // e.g. "article21"
            }

            for (key in keys) {
                if (key.isNotEmpty() && !exactIndex.containsKey(key)) {
                    exactIndex[key] = law
                }
            }
        }
    }

    fun searchLaw(query: String): List<LawResult> {
        val startTime = System.currentTimeMillis()
        val normalizedQuery = normalize(query)
        
        if (normalizedQuery.isBlank()) return emptyList()

        val results = mutableListOf<LawResult>()
        var method = "Fuzzy"

        // 1. Exact Match via Index
        val exactMatch = exactIndex[normalizedQuery]
        if (exactMatch != null) {
            results.add(exactMatch)
            method = "Exact"
            Log.d("LegalData", "Section found: ${exactMatch.sectionOrArticle}")
        }

        // 2. Exact Title Match
        if (results.isEmpty()) {
            val titleMatch = allMergedLaws.find { normalize(it.title) == normalizedQuery }
            if (titleMatch != null) {
                results.add(titleMatch)
                method = "Exact Title"
            }
        }

        // 3. Fuzzy / Partial
        if (results.isEmpty()) {
            val matches = allMergedLaws.filter { law ->
                val normSection = normalize(law.sectionOrArticle)
                val normTitle = normalize(law.title)
                
                normSection.contains(normalizedQuery) || 
                normalizedQuery.contains(normSection) ||
                normTitle.contains(normalizedQuery) ||
                normalize(law.detailedExplanation).contains(normalizedQuery)
            }.sortedWith(compareByDescending { 
                val normSection = normalize(it.sectionOrArticle)
                when {
                    normSection == normalizedQuery -> 100
                    normSection.contains(normalizedQuery) -> 80
                    normalize(it.title).contains(normalizedQuery) -> 60
                    else -> 40
                }
            })
            results.addAll(matches.take(20))
            method = "Fuzzy/Partial"
        }

        val timeTaken = System.currentTimeMillis() - startTime
        Log.d("NyayaAI", "Search method: $method, Time taken: ${timeTaken}ms, Results: ${results.size}, Query: $query")
        
        return results
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
            .replace("section", "")
            .replace("article", "")
            .replace("sec", "")
            .replace("art", "")
            .trim()
    }
    
    private fun normalizeSection(section: String): String {
        return section.filter { it.isDigit() || it.isLetter() }.lowercase()
    }

    private fun loadJsonArray(fileName: String): List<Map<String, Any>> {
        return try {
            val json = loadTextFromAsset(fileName) ?: return emptyList()
            val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
            gson.fromJson(json, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadTextFromAsset(fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }
}
