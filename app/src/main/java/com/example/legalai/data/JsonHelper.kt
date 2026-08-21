package com.example.legalai.data

import android.content.Context
import com.example.legalai.models.LawArticle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

object JsonHelper {
    fun loadLawArticles(context: Context): List<LawArticle> {
        val jsonString: String
        try {
            jsonString = context.assets.open("Alpie-core_core_indian_law.json").bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return emptyList()
        }

        val listType = object : TypeToken<List<LawArticle>>() {}.type
        return Gson().fromJson(jsonString, listType)
    }
}
