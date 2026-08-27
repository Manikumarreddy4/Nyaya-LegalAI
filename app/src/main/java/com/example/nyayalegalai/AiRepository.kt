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

class AiRepository {
    private val TAG = "AiRepository"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiService: AiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiService::class.java)
    }

    suspend fun askAi(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double = 0.4,
        maxTokens: Int = 1024,
        topP: Double = 0.9,
        isAssistant: Boolean = false
    ): String {
        if (apiKey.isEmpty() || apiKey == "NO_KEY_FOUND" || apiKey.contains("YOUR_API_KEY") || apiKey.contains("PLACEHOLDER")) {
            Log.e(TAG, "AI API key is invalid or not configured.")
            return if (isAssistant) "Assistant API key is invalid." else "Invalid AI API Key."
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
            text == "AI Server Error" || 
            text == "No Internet Connection"
        } else {
            text == "Invalid AI API Key." || 
            text == "Daily API limit reached." || 
            text == "AI Server Error." || 
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
        val request = AiChatRequest(
            model = modelName,
            messages = listOf(
                AiMessage(role = "system", content = systemPrompt),
                AiMessage(role = "user", content = userPrompt)
            ),
            temperature = temperature,
            maxTokens = maxTokens,
            topP = topP,
            stream = false
        )

        val authHeader = "Bearer $apiKey"
        val requestUrl = "https://api.groq.com/openai/v1/chat/completions"

        // Log request URL
        Log.d("AiAPI", "Request URL: $requestUrl")

        return try {
            val response = apiService.getChatCompletion(authHeader, request)
            
            // Log HTTP status code
            Log.d("AiAPI", "HTTP Status Code: ${response.code()}")

            if (response.isSuccessful) {
                val body = response.body()
                val bodyJsonString = body?.let { com.google.gson.Gson().toJson(it) } ?: ""
                
                // Log response body
                Log.d("AiAPI", "Response Body: $bodyJsonString")

                val choiceContent = body?.choices?.firstOrNull()?.message?.content
                if (!choiceContent.isNullOrBlank()) {
                    // Log parsed AI Answer
                    Log.d("AiAPI", "Parsed AI Answer: ${choiceContent.trim()}")
                    choiceContent.trim()
                } else {
                    Log.e(TAG, "Empty response from AI.")
                    if (isAssistant) "AI Server Error" else "AI Server Error."
                }
            } else {
                val errorCode = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                
                // Show exact server error in Logcat
                Log.e("AiAssistant", "Exact Server Error (Code $errorCode): $errorBody")
                Log.d("AiAPI", "Response Body (Error): $errorBody")
                Log.e(TAG, "AI API Error: Code=$errorCode")
                
                if (isAssistant) {
                    when (errorCode) {
                        401, 403 -> "Assistant API key is invalid."
                        429 -> "Daily quota exceeded."
                        else -> "AI Server Error"
                    }
                } else {
                    when (errorCode) {
                        401, 403 -> "Invalid AI API Key."
                        429 -> "Daily API limit reached."
                        else -> "AI Server Error."
                    }
                }
            }
        } catch (e: UnknownHostException) {
            Log.d("AiAPI", "No internet connection: ${e.message}")
            if (isAssistant) "No Internet Connection" else "No Internet Connection."
        } catch (e: ConnectException) {
            Log.d("AiAPI", "No internet connection: ${e.message}")
            if (isAssistant) "No Internet Connection" else "No Internet Connection."
        } catch (e: SocketTimeoutException) {
            Log.d("AiAPI", "Timeout: ${e.message}")
            if (isAssistant) "No Internet Connection" else "No Internet Connection."
        } catch (e: IOException) {
            Log.d("AiAPI", "IO error: ${e.message}")
            if (isAssistant) "No Internet Connection" else "No Internet Connection."
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}")
            if (isAssistant) "AI Server Error" else "AI Server Error."
        }
    }
}
