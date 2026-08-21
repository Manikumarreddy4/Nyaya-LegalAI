package com.example.nyayalegalai

import com.google.gson.annotations.SerializedName

data class GroqChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<GroqMessage>,
    @SerializedName("temperature") val temperature: Double = 0.4,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    @SerializedName("top_p") val topP: Double = 0.9,
    @SerializedName("stream") val stream: Boolean = false
)

data class GroqMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class GroqChatResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<GroqChoice>?,
    @SerializedName("error") val error: GroqError?
)

data class GroqChoice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: GroqMessage?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class GroqError(
    @SerializedName("message") val message: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("code") val code: String?
)
