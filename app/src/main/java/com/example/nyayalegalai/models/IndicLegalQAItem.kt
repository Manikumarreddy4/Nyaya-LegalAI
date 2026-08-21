package com.example.nyayalegalai.models

import com.google.gson.annotations.SerializedName

data class IndicLegalQAItem(
    @SerializedName("case_name") val caseName: String?,
    @SerializedName("judgment_date") val judgmentDate: String?,
    val question: String,
    val answer: String
)
