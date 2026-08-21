package com.example.nyayalegalai.model

import com.google.gson.annotations.SerializedName

data class LegalLearningItem(
    @SerializedName("prompt") val question: String,
    @SerializedName("response") val answer: String
)

data class LawArticle(
    val title: String,
    val description: String,
    val category: String,
    val content: String
)
