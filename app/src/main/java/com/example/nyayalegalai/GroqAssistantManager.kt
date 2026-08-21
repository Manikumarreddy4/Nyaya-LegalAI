package com.example.nyayalegalai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GroqAssistantManager {

    private const val TAG = "GroqAssistantManager"
    private val repository by lazy { GroqRepository() }

    suspend fun askQuestion(prompt: String): String = withContext(Dispatchers.IO) {
        Log.d("GroqAssistant", "Assistant API request started")
        Log.d("GroqAssistant", "Assistant key loaded = ${BuildConfig.GROQ_ASSISTANT_API_KEY.isNotBlank()}")

        if (BuildConfig.GROQ_ASSISTANT_API_KEY.isBlank()) {
            return@withContext "Assistant API key is missing."
        }

        // Extract raw scenario from the prompt if it contains our markers
        val scenarioKeyword = "User Scenario:"
        val formatKeyword = "You MUST respond in this EXACT format"
        val userScenario = if (prompt.contains(scenarioKeyword)) {
            val startIdx = prompt.indexOf(scenarioKeyword) + scenarioKeyword.length
            val endIdx = if (prompt.contains(formatKeyword)) prompt.indexOf(formatKeyword) else prompt.length
            prompt.substring(startIdx, endIdx).trim()
        } else {
            prompt.trim()
        }

        val systemPrompt = """
            You are an Indian AI Legal Problem Assistant.
            The user will describe a legal situation.
            Do NOT simply explain legal sections.
            Instead:
            • Understand the situation.
            • Identify the legal issue.
            • Mention applicable Indian laws when relevant.
            • Explain user rights.
            • Explain possible legal remedies.
            • Suggest practical next steps.
            • Recommend consulting a qualified lawyer when necessary.
            • Never fabricate legal facts.
            • Clearly state when information is uncertain.
            
            You MUST respond in this EXACT format. Ensure there is a double newline before every main section.
            The headers MUST be written EXACTLY as shown below, character-for-character:
            
            📌 Summary of the Issue
            [Provide a summary of the described legal problem/situation]
            
            ⚖ Possible Applicable Indian Laws
            [List the applicable Indian laws or sections relevant to this scenario]
            
            🛡 Rights of the User
            [Explain the user's legal rights in this situation]
            
            👥 Responsibilities of the Other Party
            [Explain the responsibilities of the other party involved]
            
            📝 Suggested Next Legal Steps
            [Outline recommended legal steps or remedies]
            
            👮 Police or Lawyer Recommendation
            [Advise on whether contacting the police or a lawyer is appropriate]
            
            ⚠️ Disclaimer
            This information is for informational purposes only and is not legal advice. Please consult a qualified lawyer for your specific case.
            
            STRICT RULES:
            - Do NOT simply explain legal sections.
            - Never ask the user to enter section numbers.
            - Keep your entire response within approximately 400 words.
        """.trimIndent()

        repository.askGroq(
            apiKey = BuildConfig.GROQ_ASSISTANT_API_KEY,
            systemPrompt = systemPrompt,
            userPrompt = userScenario,
            temperature = 0.4,
            maxTokens = 1024,
            topP = 0.9,
            isAssistant = true
        )
    }
}
