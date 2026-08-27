package com.example.nyayalegalai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LegalLearningManager {
    private const val TAG = "LegalLearningManager"
    private val repository by lazy { AiRepository() }

    suspend fun askLearning(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GROQ_LEARNING_API_KEY.trim()
        
        if (apiKey.isEmpty() || apiKey == "NO_KEY_FOUND" || apiKey.contains("YOUR_API_KEY") || apiKey.contains("PLACEHOLDER")) {
            Log.e(TAG, "AI Learning API key is a placeholder or invalid.")
            return@withContext "Invalid AI API Key."
        }

        val systemPrompt = "You are an expert Indian Legal AI Assistant. Explain IPC, BNS, CrPC, BNSS, Constitution and Evidence Act in simple language with sections, punishment and examples."
        repository.askAi(
            apiKey = apiKey,
            systemPrompt = systemPrompt,
            userPrompt = prompt,
            temperature = 0.4,
            maxTokens = 1024,
            topP = 0.9,
            isAssistant = false
        )
    }
}
