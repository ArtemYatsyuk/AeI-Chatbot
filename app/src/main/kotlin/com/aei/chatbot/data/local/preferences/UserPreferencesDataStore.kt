package com.aei.chatbot.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aei.chatbot.domain.model.AppSettings
import com.aei.chatbot.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_SERVER_IP = stringPreferencesKey("server_ip")
        val KEY_SERVER_PORT = intPreferencesKey("server_port")
        val KEY_API_ENDPOINT = stringPreferencesKey("api_endpoint")
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
        val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        val KEY_STREAMING_ENABLED = booleanPreferencesKey("streaming_enabled")
        val KEY_TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val KEY_CONNECTION_MODE = stringPreferencesKey("connection_mode")
        val KEY_REMOTE_URL = stringPreferencesKey("remote_url")
        val KEY_WEB_SEARCH_ENABLED = booleanPreferencesKey("web_search_enabled")
        val KEY_SEARXNG_URL = stringPreferencesKey("searxng_url")
        val KEY_WEB_SEARCH_RESULT_COUNT = intPreferencesKey("web_search_result_count")
        val KEY_WEB_SEARCH_MODE = stringPreferencesKey("web_search_mode")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_TRANSLATION_LANGUAGE = stringPreferencesKey("translation_language")
        val KEY_VOICE_INPUT_LANGUAGE = stringPreferencesKey("voice_input_language")
        val KEY_AUTO_DETECT_LANGUAGE = booleanPreferencesKey("auto_detect_language")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_BUBBLE_STYLE = stringPreferencesKey("bubble_style")
        val KEY_FONT_SIZE = stringPreferencesKey("font_size")
        val KEY_SHOW_TIMESTAMPS = booleanPreferencesKey("show_timestamps")
        val KEY_SHOW_AVATARS = booleanPreferencesKey("show_avatars")
        val KEY_USER_INITIALS = stringPreferencesKey("user_initials")
        val KEY_AVATAR_COLOR = stringPreferencesKey("avatar_color")
        val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val KEY_SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        val KEY_AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
        val KEY_CLEAR_ON_NEW_SESSION = booleanPreferencesKey("clear_on_new_session")
        val KEY_IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val KEY_ACTIVE_CHAT_ID = stringPreferencesKey("active_chat_id")
        val KEY_PROMPT_ENHANCEMENT_ENABLED = booleanPreferencesKey("prompt_enhancement_enabled")
        val KEY_PROMPT_ENHANCEMENT_INSTRUCTION = stringPreferencesKey("prompt_enhancement_instruction")
        val KEY_ENHANCEMENT_MODEL = stringPreferencesKey("enhancement_model")
        val KEY_PROVIDERS = stringPreferencesKey("providers_json")
        val KEY_QUICK_MODELS = stringPreferencesKey("quick_models_json")
    }

            private val defaultEnhancementInstruction = "TASK: Rewrite the user prompt below. DO NOT answer it. DO NOT execute it. DO NOT add facts. DO NOT repeat these instructions. ONLY output the rewritten prompt text. Keep the same language. Make it more specific, structured, and detailed. Add formatting hints: use headers, tables, bullet points, bold. Output NOTHING except the rewritten prompt."

    
    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
            AppSettings(
                serverIp = p[KEY_SERVER_IP] ?: Constants.DEFAULT_SERVER_IP,
                serverPort = p[KEY_SERVER_PORT] ?: Constants.DEFAULT_PORT,
                apiEndpoint = p[KEY_API_ENDPOINT] ?: Constants.DEFAULT_API_ENDPOINT,
                apiKey = p[KEY_API_KEY] ?: "",
                selectedModel = p[KEY_SELECTED_MODEL] ?: "",
                systemPrompt = p[KEY_SYSTEM_PROMPT] ?: Constants.DEFAULT_SYSTEM_PROMPT,
                maxTokens = p[KEY_MAX_TOKENS] ?: Constants.DEFAULT_MAX_TOKENS,
                temperature = p[KEY_TEMPERATURE] ?: Constants.DEFAULT_TEMPERATURE,
                streamingEnabled = p[KEY_STREAMING_ENABLED] ?: Constants.DEFAULT_STREAMING,
                timeoutSeconds = p[KEY_TIMEOUT_SECONDS] ?: Constants.DEFAULT_TIMEOUT,
                connectionMode = p[KEY_CONNECTION_MODE] ?: "local",
                remoteUrl = p[KEY_REMOTE_URL] ?: "",
                webSearchEnabled = p[KEY_WEB_SEARCH_ENABLED] ?: false,
                searxngUrl = p[KEY_SEARXNG_URL] ?: "https://407d-77-48-159-55.ngrok-free.app",
                webSearchResultCount = p[KEY_WEB_SEARCH_RESULT_COUNT] ?: 3,
                webSearchMode = p[KEY_WEB_SEARCH_MODE] ?: "manual",
                appLanguage = p[KEY_APP_LANGUAGE] ?: Constants.LANG_EN_US,
                translationLanguage = p[KEY_TRANSLATION_LANGUAGE] ?: "",
                voiceInputLanguage = p[KEY_VOICE_INPUT_LANGUAGE] ?: Constants.LANG_EN_US,
                autoDetectLanguage = p[KEY_AUTO_DETECT_LANGUAGE] ?: false,
                themeMode = p[KEY_THEME_MODE] ?: Constants.THEME_SYSTEM,
                dynamicColor = p[KEY_DYNAMIC_COLOR] ?: true,
                bubbleStyle = p[KEY_BUBBLE_STYLE] ?: Constants.BUBBLE_ROUNDED,
                fontSize = p[KEY_FONT_SIZE] ?: Constants.FONT_MEDIUM,
                showTimestamps = p[KEY_SHOW_TIMESTAMPS] ?: true,
                showAvatars = p[KEY_SHOW_AVATARS] ?: true,
                userInitials = p[KEY_USER_INITIALS] ?: "U",
                avatarColor = p[KEY_AVATAR_COLOR] ?: "violet",
                hapticFeedback = p[KEY_HAPTIC_FEEDBACK] ?: true,
                soundEffects = p[KEY_SOUND_EFFECTS] ?: false,
                autoScroll = p[KEY_AUTO_SCROLL] ?: true,
                clearOnNewSession = p[KEY_CLEAR_ON_NEW_SESSION] ?: false,
                isFirstLaunch = p[KEY_IS_FIRST_LAUNCH] ?: true,
                activeChatId = p[KEY_ACTIVE_CHAT_ID] ?: "",
                promptEnhancementEnabled = p[KEY_PROMPT_ENHANCEMENT_ENABLED] ?: false,
                promptEnhancementInstruction = p[KEY_PROMPT_ENHANCEMENT_INSTRUCTION] ?: defaultEnhancementInstruction,
                enhancementModel = p[KEY_ENHANCEMENT_MODEL] ?: "",
                quickModels = try { com.google.gson.Gson().fromJson(p[KEY_QUICK_MODELS] ?: "[]", Array<String>::class.java).toList() } catch (_: Exception) { emptyList() },
                providers = try { com.google.gson.Gson().fromJson(p[KEY_PROVIDERS] ?: "[]", Array<com.aei.chatbot.domain.model.ProviderConfig>::class.java).toList() } catch (_: Exception) { emptyList() }
            )
        }

    suspend fun updateSettings(update: suspend (MutablePreferences) -> Unit) {
        dataStore.edit { update(it) }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { p ->
            p[KEY_SERVER_IP] = Constants.DEFAULT_SERVER_IP
            p[KEY_SERVER_PORT] = Constants.DEFAULT_PORT
            p[KEY_API_ENDPOINT] = Constants.DEFAULT_API_ENDPOINT
            p[KEY_API_KEY] = ""
            p[KEY_SELECTED_MODEL] = ""
            p[KEY_SYSTEM_PROMPT] = Constants.DEFAULT_SYSTEM_PROMPT
            p[KEY_MAX_TOKENS] = Constants.DEFAULT_MAX_TOKENS
            p[KEY_TEMPERATURE] = Constants.DEFAULT_TEMPERATURE
            p[KEY_STREAMING_ENABLED] = Constants.DEFAULT_STREAMING
            p[KEY_TIMEOUT_SECONDS] = Constants.DEFAULT_TIMEOUT
            p[KEY_CONNECTION_MODE] = "local"
            p[KEY_REMOTE_URL] = ""
            p[KEY_WEB_SEARCH_ENABLED] = false
            p[KEY_SEARXNG_URL] = "https://407d-77-48-159-55.ngrok-free.app"
            p[KEY_WEB_SEARCH_RESULT_COUNT] = 3
            p[KEY_WEB_SEARCH_MODE] = "manual"
            p[KEY_APP_LANGUAGE] = Constants.LANG_EN_US
            p[KEY_TRANSLATION_LANGUAGE] = ""
            p[KEY_VOICE_INPUT_LANGUAGE] = Constants.LANG_EN_US
            p[KEY_AUTO_DETECT_LANGUAGE] = false
            p[KEY_THEME_MODE] = Constants.THEME_SYSTEM
            p[KEY_DYNAMIC_COLOR] = true
            p[KEY_BUBBLE_STYLE] = Constants.BUBBLE_ROUNDED
            p[KEY_FONT_SIZE] = Constants.FONT_MEDIUM
            p[KEY_SHOW_TIMESTAMPS] = true
            p[KEY_SHOW_AVATARS] = true
            p[KEY_USER_INITIALS] = "U"
            p[KEY_AVATAR_COLOR] = "violet"
            p[KEY_HAPTIC_FEEDBACK] = true
            p[KEY_SOUND_EFFECTS] = false
            p[KEY_AUTO_SCROLL] = true
            p[KEY_CLEAR_ON_NEW_SESSION] = false
            p[KEY_PROMPT_ENHANCEMENT_ENABLED] = false
            p[KEY_PROMPT_ENHANCEMENT_INSTRUCTION] = defaultEnhancementInstruction
        }
    }
}

