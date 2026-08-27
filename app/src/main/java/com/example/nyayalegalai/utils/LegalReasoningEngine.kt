package com.example.nyayalegalai.utils

import android.content.Context
import android.util.Log
import com.example.nyayalegalai.BuildConfig
import com.example.nyayalegalai.AiRepository
import com.example.nyayalegalai.models.LegalSearchResult
import com.example.nyayalegalai.repository.LocalLegalRepository

class LegalReasoningEngine(private val context: Context, private val localRepository: LocalLegalRepository) {

    private val aiRepository = AiRepository()

    suspend fun analyze(query: String): String {
        Log.d("LegalReasoningEngine", "Analyzing educational query: $query")

        // 1. Retrieve most relevant legal records from local datasets
        val localRecords = try {
            localRepository.searchLocal(query)
        } catch (e: Exception) {
            Log.e("LegalReasoningEngine", "Error searching local repository: ${e.message}")
            emptyList<LegalSearchResult>()
        }

        // 2. Prepare context from local records
        val contextText = if (localRecords.isNotEmpty()) {
            localRecords.joinToString("\n\n") { record ->
                "Section/Article: ${record.section}\nTitle: ${record.title}\nDescription: ${record.description}\nPunishment: ${record.punishment ?: "N/A"}"
            }
        } else {
            ""
        }

        val systemPrompt = "You are an expert Indian Legal AI Assistant. Explain IPC, BNS, CrPC, BNSS, Constitution and Evidence Act in simple language with sections, punishment and examples."
        val userPrompt = if (contextText.isNotBlank()) {
            "USER_QUESTION: $query\n\nLOCAL DATA CONTEXT:\n$contextText"
        } else {
            query
        }

        return try {
            Log.d("AiAPI", "Sending request to AI via LegalReasoningEngine")
            val response = aiRepository.askAi(
                apiKey = BuildConfig.GROQ_LEARNING_API_KEY,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                temperature = 0.4,
                maxTokens = 1024,
                topP = 0.9,
                isAssistant = false
            )
            
            if (response == "Invalid AI API Key." || 
                response == "Daily API limit reached." || 
                response == "AI Server Error." || 
                response == "No Internet Connection.") {
                formatLocalFallback(localRecords, query)
            } else {
                response
            }
        } catch (e: Exception) {
            Log.e("LegalReasoningEngine", "Groq Generation Error: ${e.message}")
            formatLocalFallback(localRecords, query)
        }
    }

    private fun formatLocalFallback(records: List<LegalSearchResult>, query: String): String {
        if (records.isEmpty()) {
            return """
                Section:
                $query
                
                Title:
                Not Found
                
                Meaning:
                I could not find an exact match in my offline database.
                
                Disclaimer:
                This information is for educational purposes only and is not legal advice.
            """.trimIndent()
        }

        val top = records.first()
        return """
            Section:
            ${top.section}
            
            Title:
            ${top.title}
            
            Meaning:
            ${top.description.take(150)}...
            
            Detailed Explanation:
            ${top.description}
            
            Punishment:
            ${top.punishment ?: "N/A"}
            
            Real-Life Example:
            A situation involving ${top.title} as described in the legal provision.
            
            Related Sections:
            ${records.drop(1).take(2).joinToString(", ") { it.section }}
            
            Important Points:
            - This is a local database summary.
            - Always consult official gazettes for latest updates.
            
            Disclaimer:
            This information is for educational purposes only and is not legal advice.
        """.trimIndent()
    }
}
