package com.example.nyayalegalai.data

import android.content.Context
import com.example.nyayalegalai.model.AILearningData
import com.example.nyayalegalai.model.EncyclopediaData
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.Charset

class LegalAssetManager(private val context: Context) {

    private fun loadJsonFromAsset(fileName: String): String? {
        return try {
            val inputStream: InputStream = context.assets.open(fileName)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    private fun jsonArrayToList(jsonArray: JSONArray?): List<String> {
        val list = mutableListOf<String>()
        if (jsonArray != null) {
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        }
        return list
    }

    fun getEncyclopediaData(id: String): EncyclopediaData? {
        val isIpc = id.startsWith("IPC", ignoreCase = true)
        val fileName = if (isIpc) "ipc.json" else "constitution_of_india.json"
        val jsonString = loadJsonFromAsset(fileName) ?: return null

        try {
            val jsonArray = JSONArray(jsonString)
            val searchId = id.filter { it.isDigit() }.toIntOrNull() ?: return null

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (isIpc) {
                    if (obj.optInt("Section") == searchId) {
                        return EncyclopediaData(
                            title = obj.optString("section_title"),
                            meaning = obj.optString("section_desc"),
                            punishment = "Refer to IPC details",
                            relatedSections = emptyList()
                        )
                    }
                } else {
                    if (obj.optInt("article") == searchId) {
                        return EncyclopediaData(
                            title = obj.optString("title"),
                            meaning = obj.optString("description"),
                            punishment = "N/A",
                            relatedSections = emptyList()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getAILearningData(id: String): AILearningData? {
        val fileName = "0f156bfc-84cf-46f5-830e-db21774a37f0.json"
        var jsonString = loadJsonFromAsset(fileName)
        
        // Fallback to ai_legal_learning.json if UUID file is missing
        if (jsonString == null) {
            jsonString = loadJsonFromAsset("ai_legal_learning.json") ?: return null
        }

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("id") == id) {
                    return AILearningData(
                        title = obj.optString("title"),
                        meaning = obj.optString("detailed_explanation"),
                        punishment = obj.optString("punishment"),
                        relatedSections = jsonArrayToList(obj.optJSONArray("related_sections")),
                        simpleExplanation = obj.optString("simple_explanation"),
                        realTimeExample = obj.optString("real_life_example"),
                        keywords = jsonArrayToList(obj.optJSONArray("keywords"))
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun loadAllOfflineData(): Map<String, String?> {
        return mapOf(
            "ipc" to loadJsonFromAsset("ipc.json"),
            "constitution" to loadJsonFromAsset("constitution_cleaned_dataset.json"),
            "scenarios" to loadJsonFromAsset("legal_scenarios.json"),
            "qa" to loadJsonFromAsset("IndicLegalQA Dataset_10K.json")
        )
    }
}
