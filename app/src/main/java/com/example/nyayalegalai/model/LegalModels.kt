package com.example.nyayalegalai.model

data class EncyclopediaData(
    val title: String, 
    val meaning: String, 
    val punishment: String, 
    val relatedSections: List<String>
)

data class AILearningData(
    val title: String, 
    val meaning: String, 
    val punishment: String, 
    val relatedSections: List<String>, 
    val simpleExplanation: String, 
    val realTimeExample: String,
    val keywords: List<String>
)
