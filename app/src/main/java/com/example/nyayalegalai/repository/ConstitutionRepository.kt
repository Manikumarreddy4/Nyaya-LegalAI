package com.example.nyayalegalai.repository

import android.content.Context
import com.example.nyayalegalai.model.ConstitutionArticle
import com.example.nyayalegalai.utils.AssetReader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ConstitutionRepository(
    private val context: Context
) {

    private val articles =
        mutableListOf<ConstitutionArticle>()

    init {
        loadArticles()
    }

    private fun loadArticles() {

        val json =
            AssetReader.readJson(
                context,
                "constitution_cleaned_dataset.json"
            )

        val type =
            object :
                TypeToken<List<ConstitutionArticle>>() {}.type

        val data:
                List<ConstitutionArticle> =
            Gson().fromJson(
                json,
                type
            )

        articles.addAll(data)
    }

    fun getAllArticles():
            List<ConstitutionArticle> {
        return articles
    }

    fun search(
        query: String
    ): List<ConstitutionArticle> {

        val q =
            query.lowercase()

        return articles.filter {

            it.number.contains(q) ||
                    it.title.lowercase()
                        .contains(q) ||
                    it.explanation
                        .lowercase()
                        .contains(q) ||
                    it.keywords.any {
                            keyword ->
                        keyword.lowercase()
                            .contains(q)
                    }
        }
    }
}