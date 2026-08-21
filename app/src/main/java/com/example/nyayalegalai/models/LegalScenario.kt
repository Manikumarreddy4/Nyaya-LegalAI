package com.example.nyayalegalai.models

import com.google.gson.annotations.SerializedName

data class LegalScenario(
    val keywords: List<String>,
    val category: String,
    val issue: String,
    val responsibility: String,
    val reason: String,
    @SerializedName("relevant_laws") val relevantLaws: String,
    val guidance: String
)
