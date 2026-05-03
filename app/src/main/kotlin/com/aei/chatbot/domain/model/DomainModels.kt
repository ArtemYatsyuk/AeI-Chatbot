package com.aei.chatbot.domain.model

data class ChatSession(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int,
    val lastMessage: String
)

data class ChatMessage(
    val id: String,
    val chatId: String,
    val role: String,
    val content: String,
    val translatedContent: String? = null,
    val timestamp: Long,
    val isError: Boolean = false,
    val isStreaming: Boolean = false
)

data class ModelCapabilities(
    val chat: Boolean = true,
    val vision: Boolean = false,
    val imageGeneration: Boolean = false,
    val audio: Boolean = false,
    val tools: Boolean = false,
    val webBrowsing: Boolean = false
)

data class ModelConfig(
    val id: String,
    val displayName: String,
    val modelId: String,
    val capabilities: ModelCapabilities = ModelCapabilities(),
    val contextWindow: Int = 4096,
    val maxOutputTokens: Int = 2048,
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val presencePenalty: Float = 0.0f,
    val frequencyPenalty: Float = 0.0f,
    val providerId: String = ""
)

data class ProviderConfig(
    val id: String,
    val name: String,
    val apiMode: String = "openai_compatible",
    val apiKey: String = "",
    val apiHost: String = "",
    val apiPath: String = "v1/chat/completions",
    val connectionMode: String = "local",
    val serverIp: String = "192.168.1.100",
    val serverPort: Int = 1234,
    val isEnabled: Boolean = true,
    val models: List<ModelConfig> = emptyList()
)

data class AppSettings(
    val serverIp: String = "192.168.1.100",
    val serverPort: Int = 1234,
    val apiEndpoint: String = "v1/chat/completions",
    val apiKey: String = "",
    val selectedModel: String = "",
    val systemPrompt: String = "You are AeI, a helpful, friendly, and intelligent AI assistant.",
    val maxTokens: Int = 2048,
    val temperature: Float = 0.7f,
    val streamingEnabled: Boolean = true,
    val timeoutSeconds: Int = 30,
    val connectionMode: String = "local",
    val remoteUrl: String = "",
    val webSearchEnabled: Boolean = false,
    val searxngUrl: String = "https://407d-77-48-159-55.ngrok-free.app",
    val webSearchResultCount: Int = 3,
    val webSearchMode: String = "manual",
    val safeSearch: String = "moderate",
    val defaultChatModel: String = "",
    val defaultToolsModel: String = "",
    val providers: List<ProviderConfig> = emptyList(),
    val appLanguage: String = "en-US",
    val translationLanguage: String = "",
    val voiceInputLanguage: String = "en-US",
    val autoDetectLanguage: Boolean = false,
    val themeMode: String = "system",
    val dynamicColor: Boolean = true,
    val bubbleStyle: String = "rounded",
    val fontSize: String = "medium",
    val showTimestamps: Boolean = true,
    val showAvatars: Boolean = true,
    val userInitials: String = "U",
    val avatarColor: String = "violet",
    val hapticFeedback: Boolean = true,
    val soundEffects: Boolean = false,
    val autoScroll: Boolean = true,
    val clearOnNewSession: Boolean = false,
    val isFirstLaunch: Boolean = true,
    val activeChatId: String = "",
    val promptEnhancementEnabled: Boolean = false,
    val quickModels: List<String> = emptyList(),
    val enhancementModel: String = "",
    val promptEnhancementInstruction: String = "Rewrite the following user message to be clearer, more detailed, and better structured for an AI assistant. Keep the original intent. Return ONLY the enhanced prompt, nothing else:",
    // AI Actions (Beta)
    val aiActionsEnabled: Boolean = false,
    val aiActionsAutoApprove: Boolean = false
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

data class WebSearchResult(
    val title: String,
    val url: String,
    val snippet: String
)

/** Represents an action the AI wants to perform on the device. */
sealed class AiAction {
    data class OpenApp(val packageName: String, val appLabel: String) : AiAction()
    data class CreateFile(val fileName: String, val content: String, val mimeType: String = "text/plain") : AiAction()
    data class OpenUrl(val url: String, val title: String = "") : AiAction()
    data class OpenMap(val query: String, val label: String = "") : AiAction()
}

/** State of a pending AI action awaiting user approval. */
data class PendingAiAction(
    val action: AiAction,
    val description: String
)