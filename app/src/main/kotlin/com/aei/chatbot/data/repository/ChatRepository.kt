package com.aei.chatbot.data.repository

import com.aei.chatbot.data.local.dao.ChatDao
import com.aei.chatbot.data.local.dao.MessageDao
import com.aei.chatbot.data.local.entity.ChatEntity
import com.aei.chatbot.data.local.entity.MessageEntity
import com.aei.chatbot.data.local.preferences.UserPreferencesDataStore
import com.aei.chatbot.data.remote.model.ChatRequest
import com.aei.chatbot.data.remote.model.Message
import com.aei.chatbot.data.remote.network.RetrofitClient
import com.aei.chatbot.domain.model.ApiResult
import com.aei.chatbot.domain.model.AppSettings
import com.aei.chatbot.domain.model.ChatMessage
import com.aei.chatbot.domain.model.ChatSession
import com.aei.chatbot.util.Constants
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

interface ChatRepository {
    fun getAllChats(): Flow<List<ChatSession>>
    fun searchChats(query: String): Flow<List<ChatSession>>
    fun getMessagesForChat(chatId: String): Flow<List<ChatMessage>>
    suspend fun insertChat(chat: ChatSession)
    suspend fun updateChat(chat: ChatSession)
    suspend fun deleteChat(chatId: String)
    suspend fun deleteAllChats()
    suspend fun insertMessage(message: ChatMessage)
    suspend fun updateMessage(message: ChatMessage)
    suspend fun deleteMessage(messageId: String, chatId: String)
    fun streamMessage(settings: AppSettings, messages: List<ChatMessage>): Flow<ApiResult<String>>
    suspend fun sendMessage(settings: AppSettings, messages: List<ChatMessage>): ApiResult<String>
    suspend fun getAvailableModels(settings: AppSettings): ApiResult<List<String>>
    suspend fun testConnection(settings: AppSettings): ApiResult<Unit>
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val prefsDataStore: UserPreferencesDataStore
) : ChatRepository {

    private val gson = Gson()

    private fun buildClient(settings: AppSettings) = RetrofitClient(
        serverIp = settings.serverIp,
        serverPort = settings.serverPort,
        timeoutSeconds = settings.timeoutSeconds,
        apiKey = settings.apiKey,
        connectionMode = settings.connectionMode,
        remoteUrl = settings.remoteUrl,
        apiEndpoint = settings.apiEndpoint
    )

    override fun getAllChats(): Flow<List<ChatSession>> =
        chatDao.getAllChats().map { list -> list.map { it.toDomain() } }

    override fun searchChats(query: String): Flow<List<ChatSession>> =
        chatDao.searchChats(query).map { list -> list.map { it.toDomain() } }

    override fun getMessagesForChat(chatId: String): Flow<List<ChatMessage>> =
        messageDao.getMessagesForChat(chatId).map { list -> list.map { it.toDomain() } }

    override suspend fun insertChat(chat: ChatSession) {
        withContext(Dispatchers.IO) { chatDao.insertChat(chat.toEntity()) }
    }

    override suspend fun updateChat(chat: ChatSession) {
        withContext(Dispatchers.IO) { chatDao.updateChat(chat.toEntity()) }
    }

    override suspend fun deleteChat(chatId: String) {
        withContext(Dispatchers.IO) {
            messageDao.deleteMessagesForChat(chatId)
            val entity = chatDao.getChatById(chatId)
            entity?.let { chatDao.deleteChat(it) }
        }
    }

    override suspend fun deleteAllChats() {
        withContext(Dispatchers.IO) { chatDao.deleteAllChats() }
    }

    override suspend fun insertMessage(message: ChatMessage) {
        withContext(Dispatchers.IO) {
            // Ensure parent chat exists to avoid FOREIGN KEY constraint
            val existingChat = chatDao.getChatById(message.chatId)
            if (existingChat == null) {
                chatDao.insertChat(ChatEntity(
                    id = message.chatId,
                    name = "New Chat",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    messageCount = 0,
                    lastMessage = ""
                ))
            }
            messageDao.insertMessage(message.toEntity())
        }
    }

    override suspend fun updateMessage(message: ChatMessage) {
        withContext(Dispatchers.IO) { messageDao.updateMessage(message.toEntity()) }
    }

    override suspend fun deleteMessage(messageId: String, chatId: String) {
        withContext(Dispatchers.IO) {
            val messages = messageDao.getMessagesForChatSync(chatId)
            val entity = messages.find { it.id == messageId }
            entity?.let { messageDao.deleteMessage(it) }
        }
    }

    override fun streamMessage(settings: AppSettings, messages: List<ChatMessage>): Flow<ApiResult<String>> = channelFlow {
        send(ApiResult.Loading)
        try {
            val client = buildClient(settings)
            val apiMessages = buildApiMessages(settings, messages)
            if (apiMessages.isEmpty()) {
                send(ApiResult.Error("No messages to send."))
                return@channelFlow
            }
            val chatRequest = ChatRequest(
                model = settings.selectedModel,
                messages = apiMessages,
                temperature = settings.temperature,
                maxTokens = settings.maxTokens,
                stream = true
            )
            val url = client.chatCompletionUrl
            val jsonBody = gson.toJson(chatRequest)

            android.util.Log.d("AeI", "Streaming to: $url")

            // Use raw OkHttp call to avoid Retrofit buffering
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = withContext(Dispatchers.IO) {
                client.okHttpClient.newCall(request).execute()
            }

            when {
                !response.isSuccessful -> {
                    val errorBody = try { response.body?.string()?.take(300) } catch (_: Exception) { null }
                    android.util.Log.e("AeI", "Stream error ${response.code}: $errorBody")
                    send(ApiResult.Error("${httpErrorMessage(response.code)}${if (!errorBody.isNullOrBlank()) "\n$errorBody" else ""}"))
                    response.close()
                }
                response.body == null -> {
                    send(ApiResult.Error("Empty response from server."))
                    response.close()
                }
                else -> {
                    withContext(Dispatchers.IO) {
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(response.body!!.byteStream()))
                        try {
                            var line = reader.readLine()
                            while (line != null) {
                                // SSE line received
                                when {
                                    line.startsWith("data: [DONE]") -> break
                                    line.startsWith("data: ") -> {
                                        val json = line.removePrefix("data: ").trim()
                                        if (json.isNotEmpty()) {
                                            try {
                                                val chunk = gson.fromJson(json, com.aei.chatbot.data.remote.model.StreamChunk::class.java)
                                                val delta = chunk.choices?.firstOrNull()?.delta?.content
                                                if (delta != null) {
                                                    send(ApiResult.Success(delta))
                                                }
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                android.util.Log.w("AeI", "Parse error: ${e.message}")
                                            }
                                        }
                                    }
                                }
                                line = reader.readLine()
                            }
                        } finally {
                            reader.close()
                            response.close()
                        }
                    }
                }
            }
        } catch (e: UnknownHostException) {
            send(ApiResult.Error("Cannot find server. Check URL."))
        } catch (e: ConnectException) {
            send(ApiResult.Error("Connection refused. Is server running?"))
        } catch (e: SocketTimeoutException) {
            send(ApiResult.Error("Connection timed out."))
        } catch (e: SSLException) {
            send(ApiResult.Error("SSL error: ${e.message}"))
        } catch (e: Exception) {
            send(ApiResult.Error(e.message ?: "Unexpected error."))
        }
    }.catch { e -> emit(ApiResult.Error(e.message ?: "Unexpected error.")) }

    override suspend fun sendMessage(settings: AppSettings, messages: List<ChatMessage>): ApiResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val client = buildClient(settings)
                val apiMessages = buildApiMessages(settings, messages)
                if (apiMessages.isEmpty()) return@withContext ApiResult.Error("No messages to send.")
                val request = ChatRequest(
                    model = settings.selectedModel,
                    messages = apiMessages,
                    temperature = settings.temperature,
                    maxTokens = settings.maxTokens,
                    stream = false
                )
                val url = client.chatCompletionUrl
                val response = client.apiService.chatCompletion(url, request)
                when {
                    !response.isSuccessful -> {
                        val err = try { response.errorBody()?.string()?.take(300) } catch (_: Exception) { null }
                        ApiResult.Error("${httpErrorMessage(response.code())}${if (!err.isNullOrBlank()) "\n$err" else ""}")
                    }
                    response.body() == null -> ApiResult.Error("Empty response.")
                    else -> {
                        val content = response.body()!!.choices?.firstOrNull()?.message?.content
                        if (content != null) ApiResult.Success(content) else ApiResult.Error("Empty response content.")
                    }
                }
            } catch (e: Exception) { ApiResult.Error(e.message ?: "Unexpected error.") }
        }

    override suspend fun getAvailableModels(settings: AppSettings): ApiResult<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val client = buildClient(settings)
                val url = client.modelsUrl
                val response = client.apiService.getModels(url)
                when {
                    !response.isSuccessful -> ApiResult.Error(httpErrorMessage(response.code()))
                    else -> ApiResult.Success(response.body()?.data?.map { it.id } ?: emptyList())
                }
            } catch (e: Exception) { ApiResult.Error(e.message ?: "Unexpected error.") }
        }

    override suspend fun testConnection(settings: AppSettings): ApiResult<Unit> =
        when (val r = getAvailableModels(settings)) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Error -> ApiResult.Error(r.message)
            is ApiResult.Loading -> ApiResult.Error("Unexpected state.")
        }

    private fun buildApiMessages(settings: AppSettings, messages: List<ChatMessage>): List<Message> {
        val result = mutableListOf<Message>()
        if (settings.systemPrompt.isNotBlank()) result.add(Message(Constants.ROLE_SYSTEM, settings.systemPrompt))
        messages.filter { !it.isError && it.content.isNotBlank() }.forEach { result.add(Message(it.role, it.content)) }
        if (result.isEmpty() && settings.systemPrompt.isNotBlank()) result.add(Message(Constants.ROLE_SYSTEM, settings.systemPrompt))
        return result
    }

    private fun httpErrorMessage(code: Int): String = when (code) {
        400 -> "Bad request (400)."
        401 -> "Unauthorized (401). Check API key."
        403 -> "Forbidden (403). Check permissions."
        404 -> "Not found (404). Check endpoint URL."
        422 -> "Invalid request (422). Check model name."
        429 -> "Rate limited (429). Wait and retry."
        500 -> "Server error (500)."
        503 -> "Service unavailable (503)."
        else -> "HTTP error $code."
    }

    private fun ChatEntity.toDomain() = ChatSession(id, name, createdAt, updatedAt, messageCount, lastMessage)
    private fun MessageEntity.toDomain() = ChatMessage(id, chatId, role, content, translatedContent, timestamp, isError)
    private fun ChatSession.toEntity() = ChatEntity(id, name, createdAt, updatedAt, messageCount, lastMessage)
    private fun ChatMessage.toEntity() = MessageEntity(id, chatId, role, content, translatedContent, timestamp, isError)
}
