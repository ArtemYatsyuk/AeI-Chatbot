package com.aei.chatbot.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("temperature") val temperature: Float,
    @SerializedName("max_tokens") val maxTokens: Int,
    @SerializedName("stream") val stream: Boolean,
    @SerializedName("top_p") val topP: Float = 1.0f,
    @SerializedName("frequency_penalty") val frequencyPenalty: Float = 0.0f,
    @SerializedName("presence_penalty") val presencePenalty: Float = 0.0f
)

data class Message(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<Choice>?,
    @SerializedName("model") val model: String?
)

data class Choice(
    @SerializedName("message") val message: Message?,
    @SerializedName("delta") val delta: Delta?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Delta(
    @SerializedName("content") val content: String?
)

data class StreamChunk(
    @SerializedName("choices") val choices: List<Choice>?
)

data class ModelsResponse(
    @SerializedName("data") val data: List<ModelData>?
)

data class ModelData(
    @SerializedName("id") val id: String
)
