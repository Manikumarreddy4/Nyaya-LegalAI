package com.example.nyayalegalai.data.loader

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class AssetLoader(private val context: Context) {

    /**
     * Reads a JSON file from assets and returns it as a String.
     */
    fun loadJsonString(fileName: String): String? {
        return try {
            context.assets.open(fileName).use { inputStream ->
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                String(buffer, StandardCharsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.e("AssetLoader", "Error reading JSON asset: $fileName", e)
            null
        }
    }

    /**
     * Reads a JSON file from assets and returns it as a JSONArray.
     */
    fun loadJsonArray(fileName: String): JSONArray? {
        val jsonString = loadJsonString(fileName) ?: return null
        return try {
            JSONArray(jsonString)
        } catch (e: JSONException) {
            Log.e("AssetLoader", "Error parsing JSON array: $fileName", e)
            null
        }
    }

    /**
     * Reads a JSON file from assets and returns it as a JSONObject.
     */
    fun loadJsonObject(fileName: String): org.json.JSONObject? {
        val jsonString = loadJsonString(fileName) ?: return null
        return try {
            org.json.JSONObject(jsonString)
        } catch (e: JSONException) {
            Log.e("AssetLoader", "Error parsing JSON object: $fileName", e)
            null
        }
    }

    /**
     * Reads a CSV file from assets and returns it as a list of string arrays (rows).
     */
    fun loadCsvData(fileName: String): List<Array<String>> {
        val rows = mutableListOf<Array<String>>()
        try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            var line: String? = reader.readLine()
            while (line != null) {
                // Simple CSV split, might need regex for complex cases
                val row = line.split(",").toTypedArray()
                rows.add(row)
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("AssetLoader", "Error reading CSV asset: $fileName", e)
        }
        return rows
    }
}
