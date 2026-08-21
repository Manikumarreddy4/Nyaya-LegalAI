package com.example.nyayalegalai

import com.example.nyayalegalai.models.ConstitutionArticle
import android.content.Context
import android.util.Log
import com.example.nyayalegalai.database.LawEntry
import com.example.nyayalegalai.models.AlpieLawItem
import com.example.nyayalegalai.model.LegalLearningItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import java.io.InputStreamReader

object JsonHelper {

    fun loadLawData(context: Context): List<LegalLearningItem> {
        return try {
            val json = context.assets
                .open("law_data.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<LegalLearningItem>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Provides a sequence of Alpie law data from assets to avoid OutOfMemory errors.
     * Note: The caller must ensure this is run on a background thread.
     */
    fun streamAlpieLawData(context: Context): Sequence<AlpieLawItem> = sequence {
        try {
            context.assets.open("Alpie-core_core_indian_law.json").use { inputStream ->
                JsonReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    val gson = Gson()
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val item: AlpieLawItem = gson.fromJson(reader, AlpieLawItem::class.java)
                        yield(item)
                    }
                    reader.endArray()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Parses the IPC Section CSV dataset.
     */
    fun loadIpcCsvData(context: Context): List<LawEntry> {
        val laws = mutableListOf<LawEntry>()
        try {
            context.assets.open("ipc_section_dataset_template.csv").bufferedReader().use { reader ->
                val header = reader.readLine() // Skip header
                Log.d("JsonHelper", "IPC CSV Columns: $header")
                
                var count = 0
                reader.forEachLine { line ->
                    if (line.isBlank()) return@forEachLine
                    // Robust CSV split for quotes
                    val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().removeSurrounding("\"") }
                    
                    if (tokens.size >= 2) {
                        val section = tokens.getOrNull(1) ?: ""
                        if (section.isEmpty()) return@forEachLine
                        
                        val rawTitle = tokens.getOrNull(2)
                        val rawDescription = tokens.getOrNull(3)
                        
                        // Skip if no content is available for this section
                        if (rawTitle.isNullOrBlank() && rawDescription.isNullOrBlank()) return@forEachLine
                        if ((rawDescription?.length ?: 0) < 10 && (rawTitle?.length ?: 0) < 5) return@forEachLine

                        val title = rawTitle?.takeIf { it.isNotBlank() } ?: ""
                        var description = rawDescription?.takeIf { it.isNotBlank() } ?: ""
                        var example = ""
                        
                        // Extract example if embedded in description
                        if (description.contains("Real-Life Example:", ignoreCase = true)) {
                            val parts = description.split(Regex("Real-Life Example:", RegexOption.IGNORE_CASE))
                            description = parts[0].trim()
                            example = parts.getOrNull(1)?.trim() ?: ""
                        }

                        val punishment = tokens.getOrNull(4)?.takeIf { it.isNotEmpty() } ?: ""
                        val keywords = tokens.getOrNull(5) ?: ""
                        val related = tokens.getOrNull(6) ?: ""

                        if (count < 5 && title.isNotBlank()) Log.d("JsonHelper", "Sample IPC: $section - $title")
                        count++

                        laws.add(
                            LawEntry(
                                title = title,
                                category = "Criminal Law",
                                sectionNumber = "Section $section",
                                description = description,
                                fullExplanation = description,
                                punishment = punishment,
                                example = example,
                                relatedLaws = related,
                                keywords = "ipc, section $section, criminal, $keywords, $title".lowercase()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("JsonHelper", "Error loading IPC CSV", e)
        }
        return laws
    }

    /**
     * Parses the India Law Master Index CSV dataset.
     */
    fun loadMasterIndexCsvData(context: Context): List<LawEntry> {
        val laws = mutableListOf<LawEntry>()
        try {
            context.assets.open("india_law_master_index_dataset.csv").bufferedReader().use { reader ->
                val header = reader.readLine() // Skip header
                Log.d("JsonHelper", "Master Index CSV Columns: $header")
                
                var count = 0
                reader.forEachLine { line ->
                    if (line.isBlank()) return@forEachLine
                    val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().removeSurrounding("\"") }
                    
                    if (tokens.size >= 3) {
                        val system = tokens.getOrNull(0) ?: ""
                        val type = tokens.getOrNull(1) ?: "Section"
                        val number = tokens.getOrNull(2) ?: ""
                        
                        if (number.isEmpty()) return@forEachLine

                        val rawTitle = tokens.getOrNull(3)
                        val rawDescription = tokens.getOrNull(4)

                        // Skip if no content is available for this section
                        if (rawTitle.isNullOrBlank() && rawDescription.isNullOrBlank()) return@forEachLine

                        // Normalize Category
                        val category = when {
                            system.contains("Constitution", true) -> "Constitution"
                            system.contains("IPC", true) || system.contains("CrPC", true) || system.contains("BNS", true) -> "Criminal Law"
                            system.contains("Marriage", true) || system.contains("Divorce", true) || system.contains("Family", true) -> "Family Law"
                            system.contains("Contract", true) -> "Contract Law"
                            system.contains("Consumer", true) -> "Consumer Law"
                            system.contains("CPC", true) || system.contains("Civil", true) -> "Civil Law"
                            system.contains("Cyber", true) || system.contains("IT Act", true) -> "Cyber Law"
                            system.contains("Property", true) || system.contains("Land", true) -> "Property Law"
                            system.contains("Labour", true) || system.contains("Industrial", true) -> "Labour Law"
                            system.contains("Environment", true) || system.contains("Forest", true) -> "Environmental Law"
                            else -> system
                        }
                        
                        val title = rawTitle?.takeIf { it.isNotEmpty() } ?: ""
                        val description = rawDescription?.takeIf { it.isNotEmpty() } ?: ""

                        if (count < 5 && title.isNotBlank()) Log.d("JsonHelper", "Sample Index: $number - $title")
                        count++

                        laws.add(
                            LawEntry(
                                title = title,
                                category = category,
                                sectionNumber = "$type $number",
                                description = description,
                                fullExplanation = description,
                                punishment = "",
                                example = "",
                                relatedLaws = "",
                                keywords = "$system, $category, $type, $number, $title".lowercase()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("JsonHelper", "Error loading Master Index CSV", e)
        }
        return laws
    }

    /**
     * Streams IndicLegalQA JSON data.
     */
    fun streamIndicLegalQAData(context: Context): Sequence<LawEntry> = sequence {
        try {
            context.assets.open("IndicLegalQA Dataset_10K.json").use { inputStream ->
                JsonReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    val gson = Gson()
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val item: com.example.nyayalegalai.models.IndicLegalQAItem = gson.fromJson(reader, com.example.nyayalegalai.models.IndicLegalQAItem::class.java)
                        yield(
                            LawEntry(
                                title = item.question.take(60).plus("..."),
                                category = "Legal Knowledge",
                                sectionNumber = item.caseName ?: "General",
                                description = item.answer.take(250).plus(if(item.answer.length > 250) "..." else ""),
                                fullExplanation = item.answer,
                                punishment = "Case Reference: ${item.caseName ?: "N/A"}",
                                example = "Judgment Date: ${item.judgmentDate ?: "N/A"}",
                                relatedLaws = "",
                                keywords = "indiclegalqa, ${item.caseName}, question".lowercase()
                            )
                        )
                    }
                    reader.endArray()
                }
            }
        } catch (e: Exception) {
            Log.e("JsonHelper", "Error streaming IndicLegalQA", e)
        }
    }

    /**
     * Streams Legal Scenarios JSON data.
     */
    fun streamLegalScenarios(context: Context): Sequence<LawEntry> = sequence {
        try {
            context.assets.open("legal_scenarios.json").use { inputStream ->
                JsonReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    val gson = Gson()
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val item: com.example.nyayalegalai.models.LegalScenario = gson.fromJson(reader, com.example.nyayalegalai.models.LegalScenario::class.java)
                        yield(
                            LawEntry(
                                title = item.issue.take(60),
                                category = item.category,
                                sectionNumber = "Scenario",
                                description = item.guidance.take(250),
                                fullExplanation = "Issue: ${item.issue}\n\nResponsibility: ${item.responsibility}\n\nReasoning: ${item.reason}\n\nGuidance: ${item.guidance}",
                                punishment = "Relevant Laws: ${item.relevantLaws}",
                                example = "Keywords: ${item.keywords.joinToString()}",
                                relatedLaws = item.relevantLaws,
                                keywords = "${item.category}, scenario, ${item.keywords.joinToString()}".lowercase()
                            )
                        )
                    }
                    reader.endArray()
                }
            }
        } catch (e: Exception) {
            Log.e("JsonHelper", "Error streaming Legal Scenarios", e)
        }
    }
    fun loadConstitutionData(
        context: Context
    ): List<LawEntry> {

        return try {

            val json = context.assets
                .open("constitution_cleaned_dataset.json")
                .bufferedReader()
                .use { it.readText() }

            val type =
                object :
                    TypeToken<List<ConstitutionArticle>>() {}.type

            val articles:
                    List<ConstitutionArticle> =
                Gson().fromJson(json, type)

            articles.map { article ->

                LawEntry(
                    title = article.title,
                    category = "Constitution",
                    sectionNumber = "Article ${article.number}",
                    description = article.explanation,
                    fullExplanation = article.explanation,
                    punishment = article.punishment,
                    example = article.example,
                    relatedLaws = article.exceptions,
                    keywords =
                        article.keywords.joinToString(",")
                )
            }

        } catch (e: Exception) {
            Log.e(
                "JsonHelper",
                "Error loading Constitution dataset",
                e
            )
            emptyList()
        }
    }

    fun loadJsonArray(context: Context, fileName: String): org.json.JSONArray? {
        return try {
            val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
            org.json.JSONArray(json)
        } catch (e: Exception) {
            Log.e("JsonHelper", "Error loading JSON array: $fileName", e)
            null
        }
    }

    fun loadJsonObject(context: Context, fileName: String): org.json.JSONObject? {
        return try {
            val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
            org.json.JSONObject(json)
        } catch (e: Exception) {
            Log.e("JsonHelper", "Error loading JSON object: $fileName", e)
            null
        }
    }
}
