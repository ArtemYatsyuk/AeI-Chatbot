package com.aei.chatbot.data.remote.model

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model")             val model: String,
    @SerializedName("messages")          val messages: List<Message>,
    @SerializedName("temperature")       val temperature: Float,
    @SerializedName("max_tokens")        val maxTokens: Int,
    @SerializedName("stream")            val stream: Boolean,
    @SerializedName("top_p")             val topP: Float = 1.0f,
    @SerializedName("frequency_penalty") val frequencyPenalty: Float = 0.0f,
    @SerializedName("presence_penalty")  val presencePenalty: Float = 0.0f
)

/**
 * Outgoing chat message.
 * - [isVision] = false → serialised as { "role": "...", "content": "..." }
 * - [isVision] = true  → serialised as { "role": "...", "content": [ text_part, image_part ] }
 *
 * Handled entirely by [MessageSerializer]; no Gson annotations on the content fields.
 */
class Message(
    val role: String,
    val textContent: String,
    val base64Image: String? = null,
    val mimeType: String = "image/jpeg"
) {
    val isVision: Boolean get() = base64Image != null

    /** Build the content JsonArray for vision messages */
    fun buildVisionArray(): JsonArray = JsonArray().apply {
        add(JsonObject().apply {
            addProperty("type", "text")
            addProperty("text", textContent)
        })
        add(JsonObject().apply {
            addProperty("type", "image_url")
            add("image_url", JsonObject().apply {
                addProperty("url", "data:$mimeType;base64,$base64Image")
            })
        })
    }
}

/** Build a plain-text message */
fun textMessage(role: String, text: String) = Message(role = role, textContent = text)

/** Build a vision message: text + base64 image */
fun visionMessage(
    role: String,
    text: String,
    base64Image: String,
    mimeType: String = "image/jpeg"
) = Message(role = role, textContent = text, base64Image = base64Image, mimeType = mimeType)

// ── Response models (content is always a string in API responses) ─────────────

data class ChatResponse(
    @SerializedName("id")      val id: String?,
    @SerializedName("choices") val choices: List<Choice>?,
    @SerializedName("model")   val model: String?
)

data class Choice(
    @SerializedName("message")       val message: MessageRaw?,
    @SerializedName("delta")         val delta: Delta?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class MessageRaw(
    @SerializedName("role")    val role: String?,
    @SerializedName("content") val content: String?
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