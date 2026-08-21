package com.example.legalai.models

import com.google.gson.annotations.SerializedName

data class LawArticle(
    @SerializedName("prompt") val title: String,
    @SerializedName("complex_cot") val section: String,
    @SerializedName("response") val content: String
)
