package com.example.nyayalegalai

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class GroqIntegrationTest {

    @Test
    fun testGroqAssistantApiCall() = runBlocking {
        val apiKey = BuildConfig.GROQ_ASSISTANT_API_KEY
        println("Groq Assistant API Key from BuildConfig: $apiKey")
        
        assertNotNull("Groq Assistant API key should not be null", apiKey)
        assertTrue("Groq Assistant API key should not be blank", apiKey.isNotBlank())
        
        try {
            val response = GroqAssistantManager.askQuestion("User Scenario: Someone stole my mobile phone in public transport.")
            println("Response from Groq Assistant: $response")
            assertTrue("Response should contain text", response.isNotBlank())
            assertFalse("Response should not contain error: $response", 
                response.contains("Assistant API key is invalid.") || 
                response.contains("Groq Server Error") || 
                response.contains("No Internet Connection") || 
                response.contains("Daily quota exceeded.")
            )
        } catch (e: Exception) {
            fail("Groq Assistant call threw exception: ${e.message}")
        }
    }

    @Test
    fun testGroqLearningApiCall() = runBlocking {
        val apiKey = BuildConfig.GROQ_LEARNING_API_KEY
        println("Groq Learning API Key from BuildConfig: $apiKey")
        
        assertNotNull("Groq Learning API key should not be null", apiKey)
        assertTrue("Groq Learning API key should not be blank", apiKey.isNotBlank())
        
        try {
            val response = GroqLearningManager.askLearning("Explain BNS Section 304 in simple words.")
            println("Response from Groq Learning: $response")
            assertTrue("Response should contain text", response.isNotBlank())
            assertFalse("Response should not contain error: $response", 
                response.contains("Invalid Groq API Key") || 
                response.contains("Groq Server Error") || 
                response.contains("No Internet Connection") || 
                response.contains("Daily API limit reached")
            )
        } catch (e: Exception) {
            fail("Groq Learning call threw exception: ${e.message}")
        }
    }
}
