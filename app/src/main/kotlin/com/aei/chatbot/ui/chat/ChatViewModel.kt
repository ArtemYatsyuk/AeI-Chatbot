package com.aei.chatbot.ui.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import com.aei.chatbot.domain.usecase.UpdateChatUseCase
import com.aei.chatbot.domain.usecase.LoadSettingsUseCase
import com.aei.chatbot.domain.usecase.SaveSettingsUseCase
import com.aei.chatbot.domain.usecase.SendMessageUseCase
import com.aei.chatbot.util.Constants
import com.aei.chatbot.util.TranslationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
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
    val autoSearchTriggered: Boolean = false
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private var streamingJob: Job? = null
    private var currentChatId: String = ""
    private var messagesJob: Job? = null
    private var chatInserted = false
    private var initDone = false

    // Keywords that indicate the query needs real-time/web information
    private val searchKeywords = listOf(
        "latest", "current", "today", "now", "recent", "news", "price", "weather",
        "score", "live", "trending", "update", "release", "version", "stock",
        "winner", "result", "who won", "what is the", "how much", "where is",
        "when did", "when is", "what happened", "2024", "2025", "2026"
    )

    init {
        viewModelScope.launch {
            settings.collect { s ->
                if (!initDone) {
                    initDone = true
                    if (s.activeChatId.isNotEmpty()) loadChat(s.activeChatId)
                    else createNewSession()
                }
            }
        }
    }

    fun toggleWebSearch() {
        _uiState.update { it.copy(webSearchActive = !it.webSearchActive) }
    }

    private fun shouldAutoSearch(query: String, s: AppSettings): Boolean {
        if (!s.webSearchEnabled || s.searxngUrl.isBlank()) return false
        if (s.webSearchMode != "auto") return false
        val lower = query.lowercase()
        return searchKeywords.any { lower.contains(it) }
    }

    fun loadChat(chatId: String) {
        currentChatId = chatId
        chatInserted = true
        _uiState.update { it.copy(currentChatId = chatId) }
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            getChatHistoryUseCase.messagesForChat(chatId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    fun createNewSession() {
        val id = UUID.randomUUID().toString()
        currentChatId = id
        chatInserted = false
        messagesJob?.cancel()
        _uiState.update { ChatUiState(sessionName = "New Chat", currentChatId = id) }
        viewModelScope.launch { saveSettingsUseCase { it.copy(activeChatId = id) } }
    }

    private suspend fun ensureChatInserted() {
        if (!chatInserted) {
            chatInserted = true
            insertChatUseCase(ChatSession(currentChatId, _uiState.value.sessionName,
                System.currentTimeMillis(), System.currentTimeMillis(), 0, ""))
            messagesJob?.cancel()
            messagesJob = viewModelScope.launch {
                getChatHistoryUseCase.messagesForChat(currentChatId).collect { msgs ->
                    _uiState.update { it.copy(messages = msgs) }
                }
            }
        }
    }

    fun sendMessage(userInput: String) {
        if (userInput.isBlank() || _uiState.value.isStreaming) return
        val userMsg = ChatMessage(id = UUID.randomUUID().toString(), chatId = currentChatId,
            role = Constants.ROLE_USER, content = userInput.trim(), timestamp = System.currentTimeMillis())

        viewModelScope.launch {
            ensureChatInserted()
            insertMessageUseCase(userMsg)
            kotlinx.coroutines.delay(100)
            updateChatMetadata(userMsg.content)
            _uiState.update { it.copy(isStreaming = true, streamingMessage = "", error = null,
                lastSearchResults = emptyList(), autoSearchTriggered = false) }

            val s = settings.value
            val currentMsgs = _uiState.value.messages

            // Determine if search should be used:
            // Manual: user toggled search button
            // Auto: AI decides based on query keywords
            val manualSearch = _uiState.value.webSearchActive
            val autoSearch = shouldAutoSearch(userInput.trim(), s)
            val useSearch = manualSearch || autoSearch

            val messagesWithSearch = if (useSearch && s.searxngUrl.isNotBlank()) {
                _uiState.update { it.copy(isSearching = true, autoSearchTriggered = autoSearch && !manualSearch) }
                val results = webSearchService.search(userInput.trim(), s)
                _uiState.update { it.copy(isSearching = false, lastSearchResults = results) }
                if (results.isNotEmpty()) {
                    val searchMsg = ChatMessage(id = "search_context", chatId = currentChatId,
                        role = Constants.ROLE_SYSTEM, content = webSearchService.formatResultsForPrompt(results),
                        timestamp = System.currentTimeMillis())
                    listOf(searchMsg) + currentMsgs
                } else currentMsgs
            } else currentMsgs

            if (messagesWithSearch.none { !it.isError && it.content.isNotBlank() }) {
                _uiState.update { it.copy(isStreaming = false, error = "No valid messages to send.") }
                return@launch
            }

            streamResponse(s, messagesWithSearch)
            haptic()
        }
    }

    private fun streamResponse(s: AppSettings, messages: List<ChatMessage>) {
        streamingJob = viewModelScope.launch {
            var accumulated = ""
            sendMessageUseCase.stream(s, messages).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        accumulated += result.data
                        _uiState.update { it.copy(streamingMessage = accumulated) }
                    }
                    is ApiResult.Error -> {
                        insertMessageUseCase(ChatMessage(UUID.randomUUID().toString(), currentChatId,
                            Constants.ROLE_ASSISTANT, result.message, null, System.currentTimeMillis(), isError = true))
                        _uiState.update { it.copy(isStreaming = false, streamingMessage = "",
                            error = result.message, isServerReachable = false) }
                    }
                    is ApiResult.Loading -> {}
                }
            }
            if (accumulated.isNotEmpty()) finalizeAiMessage(accumulated, s)
        }
    }

    private suspend fun finalizeAiMessage(content: String, s: AppSettings) {
        var translated: String? = null
        if (s.translationLanguage.isNotEmpty()) {
            translated = try { translationManager.translate(content, s.translationLanguage) } catch (_: Exception) { null }
        }
        insertMessageUseCase(ChatMessage(UUID.randomUUID().toString(), currentChatId,
            Constants.ROLE_ASSISTANT, content, translated, System.currentTimeMillis()))
        updateChatMetadata(content)
        _uiState.update { it.copy(isStreaming = false, streamingMessage = "", isServerReachable = true) }
        haptic()
    }

    private suspend fun updateChatMetadata(lastMsg: String) {
        val msgs = _uiState.value.messages
        val name = if (_uiState.value.sessionName == "New Chat" && msgs.size <= 2)
            lastMsg.take(40).ifBlank { "New Chat" } else _uiState.value.sessionName
        updateChatUseCase(ChatSession(currentChatId, name, System.currentTimeMillis(),
            System.currentTimeMillis(), msgs.size + 1, lastMsg.take(100)))
        _uiState.update { it.copy(sessionName = name) }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        val partial = _uiState.value.streamingMessage
        if (partial.isNotEmpty()) viewModelScope.launch { finalizeAiMessage(partial, settings.value) }
        else _uiState.update { it.copy(isStreaming = false, streamingMessage = "") }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch { deleteChatUseCase.deleteMessage(messageId, currentChatId) }
    }

    fun updateSessionName(name: String) {
        val trimmed = name.take(Constants.MAX_SESSION_NAME_LENGTH)
        _uiState.update { it.copy(sessionName = trimmed) }
        viewModelScope.launch {
            if (chatInserted) updateChatUseCase(ChatSession(currentChatId, trimmed,
                System.currentTimeMillis(), System.currentTimeMillis(), _uiState.value.messages.size,
                _uiState.value.messages.lastOrNull()?.content?.take(100) ?: ""))
        }
    }

    fun handleSpeechResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (text.isNotBlank()) viewModelScope.launch { _speechResult.emit(text) }
        }
    }

    fun dismissError() { _uiState.update { it.copy(error = null) } }

    private fun haptic() {
        if (!settings.value.hapticFeedback) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(30)
            }
        } catch (_: Exception) {}
    }
}
