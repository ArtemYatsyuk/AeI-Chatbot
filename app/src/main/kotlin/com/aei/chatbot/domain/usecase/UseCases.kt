package com.aei.chatbot.domain.usecase

import com.aei.chatbot.data.repository.ChatRepository
import com.aei.chatbot.data.repository.SettingsRepository
import com.aei.chatbot.domain.model.ApiResult
import com.aei.chatbot.domain.model.AppSettings
import com.aei.chatbot.domain.model.ChatMessage
import com.aei.chatbot.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(private val repo: ChatRepository) {
    fun stream(settings: AppSettings, messages: List<ChatMessage>, imageUri: android.net.Uri? = null): Flow<ApiResult<String>> =
        repo.streamMessage(settings, messages, imageUri)

    suspend fun send(settings: AppSettings, messages: List<ChatMessage>, imageUri: android.net.Uri? = null): ApiResult<String> =
        repo.sendMessage(settings, messages, imageUri)
}

class GetChatHistoryUseCase @Inject constructor(private val repo: ChatRepository) {
    fun allChats(): Flow<List<ChatSession>> = repo.getAllChats()
    fun search(query: String): Flow<List<ChatSession>> = repo.searchChats(query)
    fun messagesForChat(chatId: String): Flow<List<ChatMessage>> = repo.getMessagesForChat(chatId)
}

class DeleteChatUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend fun deleteChat(chatId: String) = repo.deleteChat(chatId)
    suspend fun deleteAllChats() = repo.deleteAllChats()
    suspend fun deleteMessage(messageId: String, chatId: String) = repo.deleteMessage(messageId, chatId)
}

class InsertChatUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(chat: ChatSession) = repo.insertChat(chat)
}

class InsertMessageUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(message: ChatMessage) = repo.insertMessage(message)
}

class UpdateChatUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(chat: ChatSession) = repo.updateChat(chat)
}

class SaveSettingsUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(update: suspend (AppSettings) -> AppSettings) = repo.saveSettings(update)
    suspend fun reset() = repo.resetToDefaults()
}

class LoadSettingsUseCase @Inject constructor(private val repo: SettingsRepository) {
    val settings: Flow<AppSettings> = repo.settingsFlow
}

class TranslateUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend fun getModels(settings: AppSettings): ApiResult<List<String>> = repo.getAvailableModels(settings)
    suspend fun testConnection(settings: AppSettings): ApiResult<Unit> = repo.testConnection(settings)
    /** Tests a specific model by sending a minimal real chat completion request. */
    suspend fun sendTestMessage(settings: AppSettings): ApiResult<String> =
        repo.sendMessage(settings, listOf(
            ChatMessage(
                id = "test",
                chatId = "test",
                role = "user",
                content = "Hi",
                timestamp = System.currentTimeMillis()
            )
        ))
}