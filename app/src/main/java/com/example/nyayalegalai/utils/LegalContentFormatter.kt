package com.example.nyayalegalai.utils

import com.example.nyayalegalai.BuildConfig
import com.example.nyayalegalai.GroqRepository
import com.example.nyayalegalai.models.LegalSearchResult

class LegalContentFormatter {
    private val groqRepository = GroqRepository()

    suspend fun formatLegalContent(result: LegalSearchResult): String {
        val systemPrompt = "You are an expert Indian Legal AI Assistant. Format raw legal data into simple and easy-to-understand educational explanations with Indian examples."
        val userPrompt = """
            Raw Data:
            Law/Section: ${result.section} - ${result.title}
            Description: ${result.description}
            Punishment: ${result.punishment ?: "No specific punishment mentioned."}
            
            REQUIRED OUTPUT FORMAT:
            ----------------------------------
            ${result.section} – ${result.title}
            ----------------------------------
            
            What is this?
            - Give a simple explanation in easy English. Explain the purpose of this law/section.
            
            Real-Life Example:
            - Provide one realistic Indian example that is easy for a common person to understand.
            
            Punishment:
            - Detail the punishment (imprisonment, fine, or both). If none exists, say "No specific punishment mentioned."
            
            Important Points:
            - 3 to 5 key points about this law.
            
            When is this law commonly used?
            - List common situations or scenarios where this law is applied.
            
            RULES:
            1. DO NOT show any dataset names, file names, or internal sources.
            2. Use simple, non-legal language for explanations.
            3. Ensure the example is culturally relevant to India.
            4. Keep the output clean and professional.
        """.trimIndent()

        return try {
            val response = groqRepository.askGroq(
                apiKey = BuildConfig.GROQ_LEARNING_API_KEY,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                temperature = 0.4,
                maxTokens = 1024,
                topP = 0.9,
                isAssistant = false
            )
            if (response == "Invalid Groq API Key." || 
                response == "Daily API limit reached." || 
                response == "Groq Server Error." || 
                response == "No Internet Connection.") {
                fallbackFormat(result)
            } else {
                response
            }
        } catch (e: Exception) {
            fallbackFormat(result)
        }
    }

    private fun fallbackFormat(result: LegalSearchResult): String {
        return """
            ----------------------------------
            ${result.section} – ${result.title}
            ----------------------------------
            
            What is this?
            - ${result.description}
            
            Real-Life Example:
            - [Offline mode: Example not available. Check internet connection for AI-enhanced content.]
            
            Punishment:
            - ${result.punishment ?: "No specific punishment mentioned."}
            
            Important Points:
            • Reference the section ${result.section} for details.
            
            When is this law commonly used?
            - In legal proceedings related to ${result.title}.
        """.trimIndent()
    }
}
