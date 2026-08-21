package com.example.nyayalegalai.models

data class ConstitutionArticle(
    val id: String = "",
    val type: String = "",
    val number: String = "",
    val title: String = "",
    val act_name: String = "",
    val applicability: String = "",
    val explanation: String = "",
    val example: String = "",
    val exceptions: String = "",
    val punishment: String = "",
    val keywords: List<String> = emptyList()
)