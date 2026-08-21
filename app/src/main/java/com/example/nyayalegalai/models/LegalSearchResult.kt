package com.example.nyayalegalai.models

data class LegalSearchResult(
    val section: String,
    val title: String,
    val description: String,
    val source: String,
    val punishment: String? = null
)
