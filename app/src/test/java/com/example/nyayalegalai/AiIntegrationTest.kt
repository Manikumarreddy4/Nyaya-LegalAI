package com.example.nyayalegalai

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class AiIntegrationTest {

    @Test
    fun testAssistantApiCall() = runBlocking {
        val apiKey = BuildConfig.GROQ_ASSISTANT_API_KEY
        println("Assistant API Key from BuildConfig: $apiKey")
        
        assertNotNull("Assistant API key should not be null", apiKey)
        assertTrue("Assistant API key should not be blank", apiKey.isNotBlank())
        
        try {
            val response = LegalAssistantManager.askQuestion("User Scenario: Someone stole my mobile phone in public transport.")
            println("Response from Assistant: $response")
            assertTrue("Response should contain text", response.isNotBlank())
            assertFalse("Response should not contain error: $response", 
                response.contains("Assistant API key is invalid.") || 
                response.contains("AI Server Error") || 
                response.contains("No Internet Connection") || 
                response.contains("Daily quota exceeded.")
            )
        } catch (e: Exception) {
            fail("Assistant call threw exception: ${e.message}")
        }
    }

    @Test
    fun testLearningApiCall() = runBlocking {
        val apiKey = BuildConfig.GROQ_LEARNING_API_KEY
        println("Learning API Key from BuildConfig: $apiKey")
        
        assertNotNull("Learning API key should not be null", apiKey)
        assertTrue("Learning API key should not be blank", apiKey.isNotBlank())
        
        try {
            val response = LegalLearningManager.askLearning("Explain BNS Section 304 in simple words.")
            println("Response from Learning: $response")
            assertTrue("Response should contain text", response.isNotBlank())
            assertFalse("Response should not contain error: $response", 
                response.contains("Invalid AI API Key") || 
                response.contains("AI Server Error") || 
                response.contains("No Internet Connection") || 
                response.contains("Daily API limit reached")
            )
        } catch (e: Exception) {
            fail("Learning call threw exception: ${e.message}")
        }
    }
}
