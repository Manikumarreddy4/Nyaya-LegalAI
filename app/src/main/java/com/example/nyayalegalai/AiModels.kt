package com.example.nyayalegalai

import com.google.gson.annotations.SerializedName

data class AiChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<AiMessage>,
    @SerializedName("temperature") val temperature: Double = 0.4,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    @SerializedName("top_p") val topP: Double = 0.9,
    @SerializedName("stream") val stream: Boolean = false
)

data class AiMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class AiChatResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<AiChoice>?,
    @SerializedName("error") val error: AiError?
)

data class AiChoice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: AiMessage?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class AiError(
    @SerializedName("message") val message: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("code") val code: String?
)
