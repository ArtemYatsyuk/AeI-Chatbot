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
    fun updateSafeSearch(s: String) = update { it.copy(safeSearch = s) }
    fun updateAiActionsEnabled(v: Boolean)     = update { it.copy(aiActionsEnabled = v) }
    fun updateAiActionsAutoApprove(v: Boolean)  = update { it.copy(aiActionsAutoApprove = v) }
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
    fun updatePromptEnhancementEnabled(v: Boolean) = update { it.copy(promptEnhancementEnabled = v) }
    fun addQuickModel(modelId: String) {
        update { s ->
            if (modelId.isNotBlank() && modelId !in s.quickModels) {
                s.copy(quickModels = s.quickModels + modelId)
            } else s
        }
    }

    fun removeQuickModel(modelId: String) {
        update { s -> s.copy(quickModels = s.quickModels.filter { it != modelId }) }
    }

    fun updateEnhancementModel(m: String) = update { it.copy(enhancementModel = m) }

    fun updatePromptEnhancementInstruction(s: String) = update { it.copy(promptEnhancementInstruction = s) }

    fun showAddModelDialog() = _uiState.update { it.copy(showAddModelDialog = true, editingModel = null) }
    fun showEditModelDialog(model: ModelConfig) = _uiState.update { it.copy(showEditModelDialog = true, editingModel = model) }
    fun dismissModelDialog() = _uiState.update { it.copy(showAddModelDialog = false, showEditModelDialog = false, editingModel = null) }

    fun addModel(model: ModelConfig) {
        update { s ->
            val newModel = model.copy(id = UUID.randomUUID().toString())
            val updatedProviders = if (s.providers.isEmpty()) {
                listOf(ProviderConfig(id = "default", name = "Default Provider", models = listOf(newModel)))
            } else {
                s.providers.mapIndexed { index, provider ->
                    if (index == 0) provider.copy(models = provider.models + newModel) else provider
                }
            }
            s.copy(providers = updatedProviders)
        }
        _uiState.update { it.copy(showAddModelDialog = false, editingModel = null) }
        _uiState.update { it.copy(snackbarMessage = context.getString(com.aei.chatbot.R.string.model_added) + ": ${model.displayName}") }
    }

    fun updateModel(model: ModelConfig) {
        update { s ->
            s.copy(providers = s.providers.map { provider ->
                provider.copy(models = provider.models.map { m -> if (m.id == model.id) model else m })
            })
        }
        _uiState.update { it.copy(showEditModelDialog = false, editingModel = null) }
        _uiState.update { it.copy(snackbarMessage = context.getString(com.aei.chatbot.R.string.model_updated) + ": ${model.displayName}") }
    }

    fun deleteModel(modelId: String) {
        update { s ->
            s.copy(providers = s.providers.map { provider ->
                provider.copy(models = provider.models.filter { m -> m.id != modelId })
            })
        }
        _uiState.update { it.copy(snackbarMessage = context.getString(com.aei.chatbot.R.string.model_deleted)) }
    }

    fun testModel(model: ModelConfig) {
        viewModelScope.launch {
            _uiState.update { it.copy(showTestModelDialog = true, isTestingModel = true, testModelResult = "") }
            // Use a real chat completion with the specific model ID to verify it actually exists
            val testSettings = settings.value.copy(
                selectedModel = model.modelId,
                streamingEnabled = false,
                maxTokens = 8
            )
            when (val result = translateUseCase.sendTestMessage(testSettings)) {
                is ApiResult.Success -> {
                    val guessedCaps = guessCapabilities(model.modelId)
                    val guessedCtx  = guessContextWindow(model.modelId)
                    val guessedOut  = guessMaxOutput(model.modelId)
                    val updated = model.copy(
                        capabilities    = guessedCaps,
                        contextWindow   = guessedCtx,
                        maxOutputTokens = guessedOut
                    )
                    updateModel(updated)
                    _uiState.update {
                        it.copy(isTestingModel = false,
                            testModelResult = "Connected! Context: ${guessedCtx.toKStr()}, Max Output: ${guessedOut.toKStr()}, Capabilities auto-applied for '${model.displayName}'.")
                    }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isTestingModel = false, testModelResult = "Error: ${result.message}")
                }
                else -> {}
            }
        }
    }

    private fun Int.toKStr(): String = if (this >= 1_000) "${this / 1_000}K" else "$this"

    /** Guess context window (tokens) from model ID. */
    private fun guessContextWindow(modelId: String): Int {
        val l = modelId.lowercase()
        return when {
            // Explicit token count hints in model name
            l.contains("1m")                               -> 1_000_000
            l.contains("512k")                             -> 524_288
            l.contains("256k")                             -> 262_144
            l.contains("200k")                             -> 204_800
            l.contains("128k")                             -> 131_072
            l.contains("100k")                             -> 102_400
            l.contains("64k")                              -> 65_536
            l.contains("32k")                              -> 32_768
            l.contains("16k")                              -> 16_384
            l.contains("8k")                               -> 8_192
            l.contains("4k")                               -> 4_096
            // Anthropic
            l.contains("claude-3-5") || l.contains("claude-3.5") -> 200_000
            l.contains("claude-3")                         -> 200_000
            l.contains("claude-2")                         -> 100_000
            l.contains("claude")                           -> 100_000
            // OpenAI
            l.contains("gpt-4o")                           -> 128_000
            l.contains("gpt-4-turbo")                      -> 128_000
            l.contains("gpt-4")                            -> 128_000
            l.contains("gpt-3.5-turbo")                    -> 16_385
            l.contains("o1") || l.contains("o3")           -> 200_000
            // Google
            l.contains("gemini-2.0") || l.contains("gemini-2")    -> 1_000_000
            l.contains("gemini-1.5")                       -> 1_000_000
            l.contains("gemini-1.0") || l.contains("gemini")      -> 32_760
            // Meta Llama
            l.contains("llama-3.2") || l.contains("llama3.2")     -> 131_072
            l.contains("llama-3.1") || l.contains("llama3.1")     -> 131_072
            l.contains("llama-3")   || l.contains("llama3")       -> 8_192
            l.contains("llama-2")   || l.contains("llama2")       -> 4_096
            l.contains("llama")                            -> 4_096
            // Mistral / Mixtral
            l.contains("mistral-large")                    -> 131_072
            l.contains("mistral-nemo")                     -> 128_000
            l.contains("mistral-7b") || l.contains("mixtral")     -> 32_768
            l.contains("mistral")                          -> 32_768
            l.contains("codestral")                        -> 32_768
            // Qwen
            l.contains("qwen2.5") || l.contains("qwen-2.5")       -> 131_072
            l.contains("qwen2")    || l.contains("qwen-2")        -> 131_072
            l.contains("qwen")                             -> 32_768
            // DeepSeek
            l.contains("deepseek-r1")                      -> 65_536
            l.contains("deepseek-v3") || l.contains("deepseek-v2") -> 131_072
            l.contains("deepseek")                         -> 32_768
            // Phi
            l.contains("phi-4")                            -> 16_384
            l.contains("phi-3.5")                          -> 131_072
            l.contains("phi-3")                            -> 131_072
            l.contains("phi-2")                            -> 2_048
            l.contains("phi")                              -> 4_096
            // Cohere
            l.contains("command-r-plus")                   -> 128_000
            l.contains("command-r")                        -> 128_000
            l.contains("command")                          -> 4_096
            // Yi
            l.contains("yi-34b") || l.contains("yi-9b")   -> 200_000
            l.contains("yi")                               -> 4_096
            // Gemma
            l.contains("gemma-2")                          -> 8_192
            l.contains("gemma")                            -> 8_192
            // NVIDIA / NIM
            l.contains("nemotron")                         -> 4_096
            // Falcon
            l.contains("falcon")                           -> 4_096
            // Solar
            l.contains("solar")                            -> 4_096
            // Vicuna / Alpaca
            l.contains("vicuna") || l.contains("alpaca")   -> 4_096
            else                                           -> 4_096
        }
    }

    /** Guess max output tokens from model ID. */
    private fun guessMaxOutput(modelId: String): Int {
        val l = modelId.lowercase()
        return when {
            l.contains("claude-3-5") || l.contains("claude-3.5") -> 8_192
            l.contains("claude-3")                         -> 4_096
            l.contains("claude")                           -> 4_096
            l.contains("gpt-4o-mini")                      -> 16_384
            l.contains("gpt-4o")                           -> 16_384
            l.contains("gpt-4-turbo")                      -> 4_096
            l.contains("gpt-4")                            -> 4_096
            l.contains("gpt-3.5")                          -> 4_096
            l.contains("o1") || l.contains("o3")           -> 65_536
            l.contains("gemini-2")                         -> 8_192
            l.contains("gemini-1.5")                       -> 8_192
            l.contains("gemini")                           -> 2_048
            l.contains("llama-3.1") || l.contains("llama3.1") -> 8_192
            l.contains("llama-3.2") || l.contains("llama3.2") -> 8_192
            l.contains("llama-3")   || l.contains("llama3")   -> 4_096
            l.contains("llama")                            -> 2_048
            l.contains("mistral-large")                    -> 8_192
            l.contains("mistral-nemo")                     -> 8_192
            l.contains("mixtral")                          -> 4_096
            l.contains("mistral")                          -> 4_096
            l.contains("qwen2.5") || l.contains("qwen-2.5")   -> 8_192
            l.contains("qwen2")    || l.contains("qwen-2")    -> 8_192
            l.contains("qwen")                             -> 2_048
            l.contains("deepseek-r1")                      -> 16_384
            l.contains("deepseek-v3")                      -> 8_192
            l.contains("deepseek")                         -> 4_096
            l.contains("phi-4")                            -> 4_096
            l.contains("phi-3.5")                          -> 4_096
            l.contains("phi-3")                            -> 4_096
            l.contains("phi")                              -> 2_048
            l.contains("command-r")                        -> 4_096
            l.contains("nemotron")                         -> 4_096
            l.contains("gemma-2")                          -> 8_192
            l.contains("gemma")                            -> 4_096
            else                                           -> 2_048
        }
    }

    fun dismissTestModelDialog() = _uiState.update { it.copy(showTestModelDialog = false, testModelResult = "") }

    fun resetProviderModels() {
        update { it.copy(providers = emptyList()) }
        _uiState.update { it.copy(snackbarMessage = context.getString(com.aei.chatbot.R.string.models_reset)) }
    }

    fun fetchModels() = loadModels()

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
                    val existingIds = settings.value.providers
                        .flatMap { it.models }
                        .map { it.modelId }
                        .toSet()
                    val newModels = r.data
                        .filter { it !in existingIds }
                        .map { modelId ->
                            ModelConfig(
                                id = UUID.randomUUID().toString(),
                                displayName = modelId,
                                modelId = modelId,
                                capabilities = guessCapabilities(modelId)
                            )
                        }
                    if (newModels.isNotEmpty()) {
                        update { s ->
                            val updatedProviders = if (s.providers.isEmpty()) {
                                listOf(ProviderConfig(
                                    id = UUID.randomUUID().toString(),
                                    name = "Default",
                                    models = newModels
                                ))
                            } else {
                                s.providers.mapIndexed { index, provider ->
                                    if (index == 0) provider.copy(models = provider.models + newModels)
                                    else provider
                                }
                            }
                            s.copy(providers = updatedProviders)
                        }
                    }
                }
                else -> _uiState.update { it.copy(isLoadingModels = false) }
            }
        }
    }

    /**
     * Comprehensive capability detection from model ID.
     * Covers LM Studio local models, OpenAI, Anthropic, Google, Mistral, Meta,
     * Qwen, DeepSeek, NVIDIA NIM, and generic patterns.
     */
    private fun guessCapabilities(modelId: String): ModelCapabilities {
        val l = modelId.lowercase()

        // Vision / multimodal — any model that can accept image input
        val vision = l.contains("vision") || l.contains("-vl") || l.contains("_vl") ||
            l.contains("visual") || l.contains("multimodal") || l.contains("omni") ||
            l.contains("llava") || l.contains("bakllava") || l.contains("moondream") ||
            l.contains("minicpm-v") || l.contains("cogvlm") || l.contains("internvl") ||
            l.contains("phi-3-vision") || l.contains("phi-4-vision") ||
            l.contains("gpt-4o") || l.contains("gpt-4-turbo") || // GPT-4o & turbo are multimodal
            l.contains("claude-3") ||                             // all Claude 3+ are multimodal
            l.contains("gemini") ||                               // all Gemini are multimodal
            l.contains("pixtral") ||                              // Mistral multimodal
            l.contains("qwen-vl") || l.contains("qwenvl") ||
            l.contains("qwen2-vl") || l.contains("qwen2vl") ||
            l.contains("llama-3.2") && (l.contains("11b") || l.contains("90b")) || // Llama 3.2 vision variants
            l.contains("llama3.2") && (l.contains("11b") || l.contains("90b")) ||
            l.contains("deepseek-vl") || l.contains("janus") ||
            l.contains("molmo") || l.contains("idefics") ||
            l.contains("smolvlm") || l.contains("paligemma")

        // Image generation
        val imageGen = l.contains("dall-e") || l.contains("dalle") ||
            l.contains("sdxl") || l.contains("stable-diffusion") ||
            l.contains("imagen") || l.contains("flux") ||
            l.contains("midjourney") || l.contains("kandinsky") ||
            l.contains("wuerstchen") || l.contains("aura-flow")

        // Audio (speech-to-text or text-to-speech)
        val audio = l.contains("whisper") || l.contains("tts") ||
            l.contains("speech") || l.contains("audio") ||
            l.contains("voicebox") || l.contains("musicgen") ||
            l.contains("bark") || l.contains("seamless")

        // Tool / function calling
        val tools = l.contains("instruct") || l.contains("function") ||
            l.contains("tools") || l.contains("tool-use") ||
            l.contains("agent") || l.contains("hermes") ||
            l.contains("gorilla") || l.contains("functionary") ||
            l.contains("nexusraven") || l.contains("toolbench") ||
            l.contains("gpt-4") ||   // all GPT-4 variants support tools
            l.contains("gpt-3.5-turbo") ||
            l.contains("claude-3") ||
            l.contains("gemini-1.5") || l.contains("gemini-2") ||
            l.contains("mistral-large") || l.contains("mixtral") ||
            l.contains("qwen2.5") || l.contains("qwen-2.5") ||
            l.contains("llama-3.1") || l.contains("llama-3.2") ||
            l.contains("llama3.1") || l.contains("llama3.2") ||
            l.contains("command-r") || l.contains("deepseek-v") ||
            l.contains("nemotron") || l.contains("solar")

        // Web browsing (built-in search)
        val web = l.contains("browse") || l.contains("perplexity") ||
            l.contains("search") || l.contains("internet") ||
            l.contains("you.com") || l.contains("webgpt")

        return ModelCapabilities(
            chat = true,
            vision = vision,
            imageGeneration = imageGen,
            audio = audio,
            tools = tools,
            webBrowsing = web
        )
    }

    fun exportChats() {
        viewModelScope.launch {
            try {
                val chats = getChatHistoryUseCase.allChats().first()
                val json = Gson().toJson(chats)
                val f = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "${Constants.EXPORT_FILE_PREFIX}${com.aei.chatbot.util.DateTimeUtils.formatExportTimestamp()}.json"
                )
                f.writeText(json)
                _uiState.update { it.copy(snackbarMessage = "${context.getString(com.aei.chatbot.R.string.export_success)}: ${f.name}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = context.getString(com.aei.chatbot.R.string.export_failed) + ": ${e.message}") }
            }
        }
    }

    fun clearAllChats() {
        viewModelScope.launch {
            deleteChatUseCase.deleteAllChats()
            _uiState.update { it.copy(snackbarMessage = context.getString(com.aei.chatbot.R.string.settings_clear_all_done)) }
        }
    }

    fun resetSettings() { viewModelScope.launch { saveSettingsUseCase.reset() } }

    fun importChats() {
        _uiState.update { it.copy(snackbarMessage = context.getString(com.aei.chatbot.R.string.import_coming_soon)) }
    }

    fun dismissSnackbar() { _uiState.update { it.copy(snackbarMessage = null) } }
}