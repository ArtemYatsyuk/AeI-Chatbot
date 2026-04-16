package com.aei.chatbot.ui.settings

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aei.chatbot.domain.model.ApiResult
import com.aei.chatbot.domain.model.AppSettings
import com.aei.chatbot.domain.model.ModelCapabilities
import com.aei.chatbot.domain.model.ModelConfig
import com.aei.chatbot.domain.model.ProviderConfig
import com.aei.chatbot.domain.usecase.DeleteChatUseCase
import com.aei.chatbot.domain.usecase.GetChatHistoryUseCase
import com.aei.chatbot.domain.usecase.LoadSettingsUseCase
import com.aei.chatbot.domain.usecase.SaveSettingsUseCase
import com.aei.chatbot.domain.usecase.TranslateUseCase
import com.aei.chatbot.util.Constants
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

enum class ConnectionStatus { IDLE, TESTING, SUCCESS, FAILURE }

data class SettingsUiState(
    val availableModels: List<String> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    val connectionError: String = "",
    val isLoadingModels: Boolean = false,
    val snackbarMessage: String? = null,
    val showAddModelDialog: Boolean = false,
    val showEditModelDialog: Boolean = false,
    val showTestModelDialog: Boolean = false,
    val editingModel: ModelConfig? = null,
    val showResetProviderDialog: Boolean = false,
    val testModelResult: String = "",
    val isTestingModel: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val loadSettingsUseCase: LoadSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    private val translateUseCase: TranslateUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val deleteChatUseCase: DeleteChatUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val settings: StateFlow<AppSettings> = loadSettingsUseCase.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Basic settings
    fun updateServerIp(ip: String) = update { it.copy(serverIp = ip) }
    fun updatePort(port: Int) = update { it.copy(serverPort = port) }
    fun updateApiEndpoint(ep: String) = update { it.copy(apiEndpoint = ep) }
    fun updateApiKey(key: String) = update { it.copy(apiKey = key) }
    fun updateSelectedModel(model: String) = update { it.copy(selectedModel = model) }
    fun updateSystemPrompt(p: String) = update { it.copy(systemPrompt = p) }
    fun updateMaxTokens(t: Int) = update { it.copy(maxTokens = t) }
    fun updateTemperature(t: Float) = update { it.copy(temperature = t) }
    fun updateStreamingEnabled(v: Boolean) = update { it.copy(streamingEnabled = v) }
    fun updateTimeout(t: Int) = update { it.copy(timeoutSeconds = t) }
    fun updateConnectionMode(m: String) = update { it.copy(connectionMode = m) }
    fun updateRemoteUrl(url: String) = update { it.copy(remoteUrl = url) }
    fun updateWebSearchEnabled(v: Boolean) = update { it.copy(webSearchEnabled = v) }
    fun updateWebSearchMode(m: String) = update { it.copy(webSearchMode = m) }
    fun updateSearxngUrl(url: String) = update { it.copy(searxngUrl = url) }
    fun updateWebSearchResultCount(c: Int) = update { it.copy(webSearchResultCount = c) }
    fun updateAppLanguage(l: String) = update { it.copy(appLanguage = l) }
    fun updateTranslationLanguage(l: String) = update { it.copy(translationLanguage = l) }
    fun updateVoiceInputLanguage(l: String) = update { it.copy(voiceInputLanguage = l) }
    fun updateAutoDetectLanguage(v: Boolean) = update { it.copy(autoDetectLanguage = v) }
    fun updateThemeMode(m: String) = update { it.copy(themeMode = m) }
    fun updateDynamicColor(v: Boolean) = update { it.copy(dynamicColor = v) }
    fun updateBubbleStyle(s: String) = update { it.copy(bubbleStyle = s) }
    fun updateFontSize(s: String) = update { it.copy(fontSize = s) }
    fun updateShowTimestamps(v: Boolean) = update { it.copy(showTimestamps = v) }
    fun updateShowAvatars(v: Boolean) = update { it.copy(showAvatars = v) }
    fun updateUserInitials(i: String) = update { it.copy(userInitials = i.take(2)) }
    fun updateAvatarColor(c: String) = update { it.copy(avatarColor = c) }
    fun updateHapticFeedback(v: Boolean) = update { it.copy(hapticFeedback = v) }
    fun updateSoundEffects(v: Boolean) = update { it.copy(soundEffects = v) }
    fun updateAutoScroll(v: Boolean) = update { it.copy(autoScroll = v) }
    fun updateClearOnNewSession(v: Boolean) = update { it.copy(clearOnNewSession = v) }
    fun updateDefaultChatModel(m: String) = update { it.copy(defaultChatModel = m) }
    fun updateDefaultToolsModel(m: String) = update { it.copy(defaultToolsModel = m) }

    // Model management
    fun showAddModelDialog() = _uiState.update { it.copy(showAddModelDialog = true, editingModel = null) }
    fun showEditModelDialog(model: ModelConfig) = _uiState.update { it.copy(showEditModelDialog = true, editingModel = model) }
    fun dismissModelDialog() = _uiState.update { it.copy(showAddModelDialog = false, showEditModelDialog = false, editingModel = null) }

    fun addModel(model: ModelConfig) {
        update { s ->
            val newModel = model.copy(id = UUID.randomUUID().toString())
            s.copy(providers = if (s.providers.isEmpty()) {
                listOf(ProviderConfig(id = UUID.randomUUID().toString(), name = "Default Provider",
                    apiMode = "openai_compatible", models = listOf(newModel)))
            } else {
                s.providers.mapIndexed { i, p -> if (i == 0) p.copy(models = p.models + newModel) else p }
            })
        }
        _uiState.update { it.copy(showAddModelDialog = false, editingModel = null) }
        _uiState.update { it.copy(snackbarMessage = "Model added: ${model.displayName}") }
    }

    fun updateModel(model: ModelConfig) {
        update { s ->
            s.copy(providers = s.providers.map { p ->
                p.copy(models = p.models.map { m -> if (m.id == model.id) model else m })
            })
        }
        _uiState.update { it.copy(showEditModelDialog = false, editingModel = null) }
        _uiState.update { it.copy(snackbarMessage = "Model updated: ${model.displayName}") }
    }

    fun deleteModel(modelId: String) {
        update { s ->
            s.copy(providers = s.providers.map { p ->
                p.copy(models = p.models.filter { m -> m.id != modelId })
            })
        }
        _uiState.update { it.copy(snackbarMessage = "Model deleted") }
    }

    fun testModel(model: ModelConfig) {
        viewModelScope.launch {
            _uiState.update { it.copy(showTestModelDialog = true, isTestingModel = true, testModelResult = "") }
            val s = settings.value
            when (val result = translateUseCase.testConnection(s)) {
                is ApiResult.Success -> _uiState.update { it.copy(isTestingModel = false,
                    testModelResult = "✓ Connected successfully! Model '${model.displayName}' is reachable.") }
                is ApiResult.Error -> _uiState.update { it.copy(isTestingModel = false,
                    testModelResult = "✗ Error: ${result.message}") }
                else -> {}
            }
        }
    }

    fun dismissTestModelDialog() = _uiState.update { it.copy(showTestModelDialog = false, testModelResult = "") }

    fun resetProviderModels() {
        update { it.copy(providers = emptyList()) }
        _uiState.update { it.copy(snackbarMessage = "Provider models reset") }
    }

    fun fetchModels() = loadModels()

    // All models from all providers
    fun getAllModels(): List<ModelConfig> = settings.value.providers.flatMap { it.models }

    private fun update(block: (AppSettings) -> AppSettings) {
        viewModelScope.launch { saveSettingsUseCase { block(it) } }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.TESTING) }
            when (val r = translateUseCase.testConnection(settings.value)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(connectionStatus = ConnectionStatus.SUCCESS) }
                    loadModels()
                    kotlinx.coroutines.delay(Constants.CONNECT_TEST_RESET_MS)
                    _uiState.update { it.copy(connectionStatus = ConnectionStatus.IDLE) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(connectionStatus = ConnectionStatus.FAILURE, connectionError = r.message) }
                    kotlinx.coroutines.delay(Constants.CONNECT_TEST_RESET_MS)
                    _uiState.update { it.copy(connectionStatus = ConnectionStatus.IDLE) }
                }
                else -> {}
            }
        }
    }

    fun loadModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true) }
            when (val r = translateUseCase.getModels(settings.value)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(availableModels = r.data, isLoadingModels = false) }
                    // Auto-add fetched models as ModelConfig if not already present
                    val existing = settings.value.providers.flatMap { p -> p.models.map { m -> m.modelId } }.toSet()
                    val newModels = r.data.filter { it !in existing }.map { modelId ->
                        ModelConfig(id = UUID.randomUUID().toString(), displayName = modelId, modelId = modelId,
                            capabilities = guessCapabilities(modelId))
                    }
                    if (newModels.isNotEmpty()) {
                        update { s ->
                            s.copy(providers = if (s.providers.isEmpty()) {
                                listOf(ProviderConfig(id = UUID.randomUUID().toString(), name = "Default",
                                    models = newModels))
                            } else {
                                s.providers.mapIndexed { i, p -> if (i == 0) p.copy(models = p.models + newModels) else p }
                            })
                        }
                    }
                }
                else -> _uiState.update { it.copy(isLoadingModels = false) }
            }
        }
    }

    private fun guessCapabilities(modelId: String): ModelCapabilities {
        val lower = modelId.lowercase()
        return ModelCapabilities(
            chat = true,
            vision = lower.contains("vision") || lower.contains("vl") || lower.contains("visual"),
            imageGeneration = lower.contains("dall") || lower.contains("sdxl") || lower.contains("imagen"),
            audio = lower.contains("whisper") || lower.contains("tts") || lower.contains("audio"),
            tools = lower.contains("function") || lower.contains("tools") || lower.contains("instruct"),
            webBrowsing = lower.contains("browse") || lower.contains("web")
        )
    }

    fun exportChats() {
        viewModelScope.launch {
            try {
                val chats = getChatHistoryUseCase.allChats().first()
                val json = Gson().toJson(chats)
                val f = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "${Constants.EXPORT_FILE_PREFIX}${com.aei.chatbot.util.DateTimeUtils.formatExportTimestamp()}.json")
                f.writeText(json)
                _uiState.update { it.copy(snackbarMessage = "${context.getString(com.aei.chatbot.R.string.export_success)}: ${f.name}") }
            } catch (e: Exception) { _uiState.update { it.copy(snackbarMessage = "Export failed: ${e.message}") } }
        }
    }

    fun clearAllChats() {
        viewModelScope.launch {
            deleteChatUseCase.deleteAllChats()
            _uiState.update { it.copy(snackbarMessage = context.getString(com.aei.chatbot.R.string.settings_clear_all_done)) }
        }
    }

    fun resetSettings() { viewModelScope.launch { saveSettingsUseCase.reset() } }
    fun dismissSnackbar() { _uiState.update { it.copy(snackbarMessage = null) } }
}
