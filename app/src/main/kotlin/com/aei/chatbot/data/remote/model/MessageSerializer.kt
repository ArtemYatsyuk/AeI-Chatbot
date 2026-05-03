package com.aei.chatbot.data.remote.model

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Serialises [Message] to either:
 *   { "role": "user", "content": "hello" }           — text-only
 *   { "role": "user", "content": [ {...}, {...} ] }   — vision / multipart
 */
class MessageSerializer : JsonSerializer<Message> {
    override fun serialize(
        src: Message,
        typeOfSrc: Type,
        ctx: JsonSerializationContext
    ): JsonElement = JsonObject().apply {
        addProperty("role", src.role)
        if (src.isVision) {
            add("content", src.buildVisionArray())
        } else {
            addProperty("content", src.textContent)
        }
    }
}