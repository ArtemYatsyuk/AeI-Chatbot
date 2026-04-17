package com.aei.chatbot.ui.chat

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aei.chatbot.data.remote.search.WebSearchService
import com.aei.chatbot.domain.model.ApiResult
import com.aei.chatbot.domain.model.AppSettings
import com.aei.chatbot.domain.model.ChatMessage
import com.aei.chatbot.domain.model.ChatSession
import com.aei.chatbot.domain.model.WebSearchResult
import com.aei.chatbot.domain.usecase.DeleteChatUseCase
import com.aei.chatbot.domain.usecase.GetChatHistoryUseCase
import com.aei.chatbot.domain.usecase.InsertChatUseCase
import com.aei.chatbot.domain.usecase.InsertMessageUseCase
import com.aei.chatbot.domain.usecase.LoadSettingsUseCase
import com.aei.chatbot.domain.usecase.SaveSettingsUseCase
import com.aei.chatbot.domain.usecase.SendMessageUseCase
import com.aei.chatbot.domain.usecase.UpdateChatUseCase
import com.aei.chatbot.util.Constants
import com.aei.chatbot.util.TranslationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val streamingMessage: String = "",
    val error: String? = null,
    val isServerReachable: Boolean = true,
    val sessionName: String = "New Chat",
    val currentChatId: String = "",
    val isSearching: Boolean = false,
    val webSearchActive: Boolean = false,
    val lastSearchResults: List<WebSearchResult> = emptyList(),
    val autoSearchTriggered: Boolean = false,
    val isEnhancingPrompt: Boolean = false,
    val enhancedPrompt: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val deleteChatUseCase: DeleteChatUseCase,
    private val insertChatUseCase: InsertChatUseCase,
    private val insertMessageUseCase: InsertMessageUseCase,
    private val updateChatUseCase: UpdateChatUseCase,
    private val loadSettingsUseCase: LoadSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    private val translationManager: TranslationManager,
    private val webSearchService: WebSearchService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _speechResult = MutableSharedFlow<String>()
    val speechResult: SharedFlow<String> = _speechResult.asSharedFlow()

    val settings: StateFlow<AppSettings> = loadSettingsUseCase.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private var streamingJob: Job? = null
    private var currentChatId: String = ""
    private var messagesJob: Job? = null
    private var chatInserted = false
    private var initDone = false

    private val searchKeywords = listOf(
        "latest", "current", "today", "now", "recent", "news", "price", "weather",
        "score", "live", "trending", "update", "release", "version", "stock",
        "winner", "result", "who won", "what is the", "how much", "where is",
        "when did", "when is", "what happened", "2024", "2025", "2026"
    )

    private val defaultEnhancementInstruction =
        "You are a master prompt engineer. Analyze and enhance the following prompt for generative AI. Add context, structure, specificity. Instruct the AI to use headers, tables for comparisons, bullet points for lists, bold for key terms. Return only the enhanced version. Do not ask anything else, answer only!"

    init {
        viewModelScope.launch {
            settings.collectLatest { appSettings ->
                if (!initDone) {
                    initDone = true
                    if (appSettings.activeChatId.isNotEmpty()) {
                        loadChat(appSettings.activeChatId)
                    } else {
                        createNewSession()
                    }
                }
            }
        }
    }

    fun toggleWebSearch() {
        _uiState.value = _uiState.value.copy(webSearchActive = !_uiState.value.webSearchActive)
    }

    private fun shouldAutoSearch(query: String, appSettings: AppSettings): Boolean {
        if (!appSettings.webSearchEnabled || appSettings.searxngUrl.isBlank()) return false
        if (appSettings.webSearchMode != "auto") return false
        val lowerQuery = query.lowercase()
        return searchKeywords.any { lowerQuery.contains(it) }
    }

    private suspend fun enhancePromptClean(userInput: String, appSettings: AppSettings): String {
        val instruction = appSettings.promptEnhancementInstruction.ifBlank {
            defaultEnhancementInstruction
        }

        val cleanSettings = appSettings.copy(
            systemPrompt = "",
            webSearchEnabled = false,
            webSearchMode = "manual",
            streamingEnabled = false,
            searxngUrl = ""
        )

        val enhancementMessages = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                chatId = "enhancement_temp",
                role = Constants.ROLE_USER,
                content = "$instruction\n\nOriginal prompt: \"$userInput\"",
                timestamp = System.currentTimeMillis()
            )
        )

        return when (val result = sendMessageUseCase.send(cleanSettings, enhancementMessages)) {
            is ApiResult.Success -> {
                val enhanced = result.data.trim()
                    .removePrefix("Enhanced prompt:")
                    .removePrefix("Enhanced version:")
                    .removePrefix("Enhanced:")
                    .trim()
                    .removeSurrounding("\"")
                if (enhanced.isNotBlank() && enhanced != userInput) enhanced else userInput
            }
            else -> userInput
        }
    }

    fun loadChat(chatId: String) {
        currentChatId = chatId
        chatInserted = true
        _uiState.value = _uiState.value.copy(currentChatId = chatId)

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            getChatHistoryUseCase.messagesForChat(chatId).collectLatest { loadedMessages ->
                _uiState.value = _uiState.value.copy(messages = loadedMessages)
            }
        }
    }

    fun createNewSession() {
        val newId = UUID.randomUUID().toString()
        currentChatId = newId
        chatInserted = false
        messagesJob?.cancel()
        _uiState.value = ChatUiState(sessionName = "New Chat", currentChatId = newId)

        viewModelScope.launch {
            saveSettingsUseCase { currentSettings ->
                currentSettings.copy(activeChatId = newId)
            }
        }
    }

    private suspend fun ensureChatInserted() {
        if (!chatInserted) {
            chatInserted = true
            insertChatUseCase(
                ChatSession(
                    id = currentChatId,
                    name = _uiState.value.sessionName,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    messageCount = 0,
                    lastMessage = ""
                )
            )

            messagesJob?.cancel()
            messagesJob = viewModelScope.launch {
                getChatHistoryUseCase.messagesForChat(currentChatId).collectLatest { loadedMessages ->
                    _uiState.value = _uiState.value.copy(messages = loadedMessages)
                }
            }
        }
    }

    fun sendMessage(userInput: String) {
        if (userInput.isBlank() || _uiState.value.isStreaming) return

        viewModelScope.launch {
            ensureChatInserted()
            val appSettings = settings.value

            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                chatId = currentChatId,
                role = Constants.ROLE_USER,
                content = userInput.trim(),
                timestamp = System.currentTimeMillis()
            )
            insertMessageUseCase(userMessage)
            kotlinx.coroutines.delay(100)
            updateChatMetadata(userMessage.content)
            haptic()

            var processedInput = userInput.trim()
            if (appSettings.promptEnhancementEnabled) {
                _uiState.value = _uiState.value.copy(isEnhancingPrompt = true, enhancedPrompt = null)

                val enhanced = enhancePromptClean(userInput.trim(), appSettings)
                if (enhanced.isNotBlank() && enhanced != userInput.trim()) {
                    processedInput = enhanced

                    val enhancedMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        chatId = currentChatId,
                        role = "enhanced",
                        content = enhanced,
                        timestamp = System.currentTimeMillis()
                    )
                    insertMessageUseCase(enhancedMessage)
                    kotlinx.coroutines.delay(150)
                }

                _uiState.value = _uiState.value.copy(
                    isEnhancingPrompt = false,
                    enhancedPrompt = if (processedInput != userInput.trim()) processedInput else null
                )
            }

            _uiState.value = _uiState.value.copy(
                isStreaming = true,
                streamingMessage = "",
                error = null,
                lastSearchResults = emptyList(),
                autoSearchTriggered = false
            )

            val currentMessages = _uiState.value.messages.filter { it.role != "enhanced" }
            val apiMessages =
                if (appSettings.promptEnhancementEnabled && processedInput != userInput.trim()) {
                    val lastUserIndex = currentMessages.indexOfLast { it.role == Constants.ROLE_USER }
                    if (lastUserIndex != -1) {
                        currentMessages.toMutableList().also { list ->
                            list[lastUserIndex] = list[lastUserIndex].copy(content = processedInput)
                        }
                    } else {
                        currentMessages
                    }
                } else {
                    currentMessages
                }

            val manualSearch = _uiState.value.webSearchActive
            val autoSearch = shouldAutoSearch(processedInput, appSettings)
            val useSearch = manualSearch || autoSearch

            val messagesWithSearch =
                if (useSearch && appSettings.searxngUrl.isNotBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isSearching = true,
                        autoSearchTriggered = autoSearch && !manualSearch
                    )

                    val results = webSearchService.search(processedInput, appSettings)

                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        lastSearchResults = results
                    )

                    if (results.isNotEmpty()) {
                        val searchMessage = ChatMessage(
                            id = "search_context",
                            chatId = currentChatId,
                            role = Constants.ROLE_SYSTEM,
                            content = webSearchService.formatResultsForPrompt(results),
                            timestamp = System.currentTimeMillis()
                        )
                        listOf(searchMessage) + apiMessages
                    } else {
                        apiMessages
                    }
                } else {
                    apiMessages
                }

            if (messagesWithSearch.none { !it.isError && it.content.isNotBlank() }) {
                _uiState.value = _uiState.value.copy(
                    isStreaming = false,
                    error = "No valid messages to send."
                )
                return@launch
            }

            streamResponse(appSettings, messagesWithSearch)
        }
    }

    private fun streamResponse(appSettings: AppSettings, messages: List<ChatMessage>) {
        streamingJob = viewModelScope.launch {
            var accumulated = ""
            sendMessageUseCase.stream(appSettings, messages).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        accumulated += result.data
                        _uiState.value = _uiState.value.copy(streamingMessage = accumulated)
                    }
                    is ApiResult.Error -> {
                        insertMessageUseCase(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                chatId = currentChatId,
                                role = Constants.ROLE_ASSISTANT,
                                content = result.message,
                                timestamp = System.currentTimeMillis(),
                                isError = true
                            )
                        )
                        _uiState.value = _uiState.value.copy(
                            isStreaming = false,
                            streamingMessage = "",
                            error = result.message,
                            isServerReachable = false
                        )
                    }
                    is ApiResult.Loading -> Unit
                }
            }

            if (accumulated.isNotEmpty()) {
                finalizeAiMessage(accumulated, appSettings)
            }
        }
    }

    private suspend fun finalizeAiMessage(content: String, appSettings: AppSettings) {
        val translatedContent =
            if (appSettings.translationLanguage.isNotEmpty()) {
                try {
                    translationManager.translate(content, appSettings.translationLanguage)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

        insertMessageUseCase(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                chatId = currentChatId,
                role = Constants.ROLE_ASSISTANT,
                content = content,
                translatedContent = translatedContent,
                timestamp = System.currentTimeMillis()
            )
        )

        updateChatMetadata(content)
        _uiState.value = _uiState.value.copy(
            isStreaming = false,
            streamingMessage = "",
            isServerReachable = true
        )
        haptic()
    }

    private suspend fun updateChatMetadata(lastMessage: String) {
        val messageList = _uiState.value.messages
        val sessionName =
            if (_uiState.value.sessionName == "New Chat" && messageList.size <= 2) {
                lastMessage.take(40).ifBlank { "New Chat" }
            } else {
                _uiState.value.sessionName
            }

        updateChatUseCase(
            ChatSession(
                id = currentChatId,
                name = sessionName,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                messageCount = messageList.size + 1,
                lastMessage = lastMessage.take(100)
            )
        )

        _uiState.value = _uiState.value.copy(sessionName = sessionName)
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        val partial = _uiState.value.streamingMessage
        if (partial.isNotEmpty()) {
            viewModelScope.launch { finalizeAiMessage(partial, settings.value) }
        } else {
            _uiState.value = _uiState.value.copy(isStreaming = false, streamingMessage = "")
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            deleteChatUseCase.deleteMessage(messageId, currentChatId)
        }
    }

    fun updateSessionName(name: String) {
        val trimmed = name.take(Constants.MAX_SESSION_NAME_LENGTH)
        _uiState.value = _uiState.value.copy(sessionName = trimmed)

        viewModelScope.launch {
            if (chatInserted) {
                updateChatUseCase(
                    ChatSession(
                        id = currentChatId,
                        name = trimmed,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        messageCount = _uiState.value.messages.size,
                        lastMessage = _uiState.value.messages.lastOrNull()?.content?.take(100) ?: ""
                    )
                )
            }
        }
    }

    fun handleSpeechResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val text =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                    .orEmpty()

            if (text.isNotBlank()) {
                viewModelScope.launch { _speechResult.emit(text) }
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun haptic() {
        if (!settings.value.hapticFeedback) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(30)
            }
        } catch (_: Exception) {
        }
    }
}
