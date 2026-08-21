package com.example.nyayalegalai

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class GroqRepository {
    private val TAG = "GroqRepository"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiService: GroqService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqService::class.java)
    }

    suspend fun askGroq(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double = 0.4,
        maxTokens: Int = 1024,
        topP: Double = 0.9,
        isAssistant: Boolean = false
    ): String {
        if (apiKey.isEmpty() || apiKey == "NO_KEY_FOUND" || apiKey.contains("YOUR_API_KEY") || apiKey.contains("PLACEHOLDER")) {
            Log.e(TAG, "Groq API key is invalid or not configured.")
            return if (isAssistant) "Assistant API key is invalid." else "Invalid Groq API Key."
        }

        // Try primary model
        val responseText = executeRequest(apiKey, "groq/compound-mini", systemPrompt, userPrompt, temperature, maxTokens, topP, isAssistant)
        if (isErrorResponse(responseText, isAssistant)) {
            Log.d(TAG, "Primary model failed or unavailable. Falling back to llama-3.1-8b-instant.")
            val fallbackResponse = executeRequest(apiKey, "llama-3.1-8b-instant", systemPrompt, userPrompt, temperature, maxTokens, topP, isAssistant)
            return fallbackResponse
        }
        return responseText
    }

    private fun isErrorResponse(text: String, isAssistant: Boolean): Boolean {
        return if (isAssistant) {
            text == "Assistant API key is invalid." || 
            text == "Daily quota exceeded." || 
            text == "Groq Server Error" || 
            text == "No Internet Connection"
        } else {
            text == "Invalid Groq API Key." || 
            text == "Daily API limit reached." || 
            text == "Groq Server Error." || 
            text == "No Internet Connection."
        }
    }

    private suspend fun executeRequest(
        apiKey: String,
        modelName: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double,
        maxTokens: Int,
        topP: Double,
        isAssistant: Boolean
    ): String {
        val request = GroqChatRequest(
            model = modelName,
            messages = listOf(
                GroqMessage(role = "system", content = systemPrompt),
                GroqMessage(role = "user", content = userPrompt)
            ),
            temperature = temperature,
            maxTokens = maxTokens,
            topP = topP,
            stream = false
        )

        val authHeader = "Bearer $apiKey"
        val requestUrl = "https://api.groq.com/openai/v1/chat/completions"

        // Log request URL
        Log.d("GroqAPI", "Request URL: $requestUrl")

        return try {
            val response = apiService.getChatCompletion(authHeader, request)
            
            // Log HTTP status code
            Log.d("GroqAPI", "HTTP Status Code: ${response.code()}")

            if (response.isSuccessful) {
                val body = response.body()
                val bodyJsonString = body?.let { com.google.gson.Gson().toJson(it) } ?: ""
                
                // Log response body
                Log.d("GroqAPI", "Response Body: $bodyJsonString")

                val choiceContent = body?.choices?.firstOrNull()?.message?.content
                if (!choiceContent.isNullOrBlank()) {
                    // Log parsed AI Answer
                    Log.d("GroqAPI", "Parsed AI Answer: ${choiceContent.trim()}")
                    choiceContent.trim()
                } else {
                    Log.e(TAG, "Empty response from Groq.")
                    if (isAssistant) "Groq Server Error" else "Groq Server Error."
                }
            } else {
                val errorCode = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                
                // Show exact server error in Logcat
                Log.e("GroqAssistant", "Exact Server Error (Code $errorCode): $errorBody")
                Log.d("GroqAPI", "Response Body (Error): $errorBody")
                Log.e(TAG, "Groq API Error: Code=$errorCode")
                
                if (isAssistant) {
                    when (errorCode) {
                        401, 403 -> "Assistant API key is invalid."
                        429 -> "Daily quota exceeded."
                        else -> "Groq Server Error"
                    }
                } else {
                    when (errorCode) {
                        401, 403 -> "Invalid Groq API Key."
                        429 -> "Daily API limit reached."
                        else -> "Groq Server Error."
                    }
                }
            }
        } catch (e: UnknownHostException) {
            Log.d("GroqAPI", "No internet connection: ${e.message}")
            if (isAssistant) "No Internet Connection" else "No Internet Connection."
        } catch (e: ConnectException) {
            Log.d("GroqAPI", "No internet connection: ${e.message}")
            if (isAssistant) "No Internet Connection" else "No Internet Connection."
        } catch (e: SocketTimeoutException) {
            Log.d("GroqAPI", "Timeout: ${e.message}")
            if (isAssistant) "No Internet Connection" else "No Internet Connection."
        } catch (e: IOException) {
            Log.d("GroqAPI", "IO error: ${e.message}")
            if (isAssistant) "No Internet Connection" else "No Internet Connection."
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}")
            if (isAssistant) "Groq Server Error" else "Groq Server Error."
        }
    }
}
