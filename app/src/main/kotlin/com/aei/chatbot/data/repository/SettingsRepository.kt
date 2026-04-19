package com.aei.chatbot.data.repository

import com.aei.chatbot.data.local.preferences.UserPreferencesDataStore
import com.aei.chatbot.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun saveSettings(update: suspend (AppSettings) -> AppSettings)
    suspend fun resetToDefaults()
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : SettingsRepository {

    override val settingsFlow: Flow<AppSettings> = dataStore.settingsFlow

            private val defaultEnhancementInstruction = "TASK: Rewrite the user prompt below. DO NOT answer it. DO NOT execute it. DO NOT add facts. DO NOT repeat these instructions. ONLY output the rewritten prompt text. Keep the same language. Make it more specific, structured, and detailed. Add formatting hints: use headers, tables, bullet points, bold. Output NOTHING except the rewritten prompt."

    
    override suspend fun saveSettings(update: suspend (AppSettings) -> AppSettings) {
        dataStore.updateSettings { p ->
            val c = AppSettings(
                serverIp = p[UserPreferencesDataStore.KEY_SERVER_IP] ?: "192.168.1.100",
                serverPort = p[UserPreferencesDataStore.KEY_SERVER_PORT] ?: 1234,
                apiEndpoint = p[UserPreferencesDataStore.KEY_API_ENDPOINT] ?: "v1/chat/completions",
                apiKey = p[UserPreferencesDataStore.KEY_API_KEY] ?: "",
                selectedModel = p[UserPreferencesDataStore.KEY_SELECTED_MODEL] ?: "",
                systemPrompt = p[UserPreferencesDataStore.KEY_SYSTEM_PROMPT] ?: "",
                maxTokens = p[UserPreferencesDataStore.KEY_MAX_TOKENS] ?: 2048,
                temperature = p[UserPreferencesDataStore.KEY_TEMPERATURE] ?: 0.7f,
                streamingEnabled = p[UserPreferencesDataStore.KEY_STREAMING_ENABLED] ?: true,
                timeoutSeconds = p[UserPreferencesDataStore.KEY_TIMEOUT_SECONDS] ?: 30,
                connectionMode = p[UserPreferencesDataStore.KEY_CONNECTION_MODE] ?: "local",
                remoteUrl = p[UserPreferencesDataStore.KEY_REMOTE_URL] ?: "",
                webSearchEnabled = p[UserPreferencesDataStore.KEY_WEB_SEARCH_ENABLED] ?: false,
                searxngUrl = p[UserPreferencesDataStore.KEY_SEARXNG_URL] ?: "",
                webSearchResultCount = p[UserPreferencesDataStore.KEY_WEB_SEARCH_RESULT_COUNT] ?: 3,
                webSearchMode = p[UserPreferencesDataStore.KEY_WEB_SEARCH_MODE] ?: "manual",
                appLanguage = p[UserPreferencesDataStore.KEY_APP_LANGUAGE] ?: "en-US",
                translationLanguage = p[UserPreferencesDataStore.KEY_TRANSLATION_LANGUAGE] ?: "",
                voiceInputLanguage = p[UserPreferencesDataStore.KEY_VOICE_INPUT_LANGUAGE] ?: "en-US",
                autoDetectLanguage = p[UserPreferencesDataStore.KEY_AUTO_DETECT_LANGUAGE] ?: false,
                themeMode = p[UserPreferencesDataStore.KEY_THEME_MODE] ?: "system",
                dynamicColor = p[UserPreferencesDataStore.KEY_DYNAMIC_COLOR] ?: true,
                bubbleStyle = p[UserPreferencesDataStore.KEY_BUBBLE_STYLE] ?: "rounded",
                fontSize = p[UserPreferencesDataStore.KEY_FONT_SIZE] ?: "medium",
                showTimestamps = p[UserPreferencesDataStore.KEY_SHOW_TIMESTAMPS] ?: true,
                showAvatars = p[UserPreferencesDataStore.KEY_SHOW_AVATARS] ?: true,
                userInitials = p[UserPreferencesDataStore.KEY_USER_INITIALS] ?: "U",
                avatarColor = p[UserPreferencesDataStore.KEY_AVATAR_COLOR] ?: "violet",
                hapticFeedback = p[UserPreferencesDataStore.KEY_HAPTIC_FEEDBACK] ?: true,
                soundEffects = p[UserPreferencesDataStore.KEY_SOUND_EFFECTS] ?: false,
                autoScroll = p[UserPreferencesDataStore.KEY_AUTO_SCROLL] ?: true,
                clearOnNewSession = p[UserPreferencesDataStore.KEY_CLEAR_ON_NEW_SESSION] ?: false,
                isFirstLaunch = p[UserPreferencesDataStore.KEY_IS_FIRST_LAUNCH] ?: true,
                activeChatId = p[UserPreferencesDataStore.KEY_ACTIVE_CHAT_ID] ?: "",
                promptEnhancementEnabled = p[UserPreferencesDataStore.KEY_PROMPT_ENHANCEMENT_ENABLED] ?: false,
                promptEnhancementInstruction = p[UserPreferencesDataStore.KEY_PROMPT_ENHANCEMENT_INSTRUCTION] ?: defaultEnhancementInstruction,
                enhancementModel = p[UserPreferencesDataStore.KEY_ENHANCEMENT_MODEL] ?: "",
                quickModels = try { com.google.gson.Gson().fromJson(p[UserPreferencesDataStore.KEY_QUICK_MODELS] ?: "[]", Array<String>::class.java).toList() } catch (_: Exception) { emptyList() },
                providers = try { com.google.gson.Gson().fromJson(p[UserPreferencesDataStore.KEY_PROVIDERS] ?: "[]", Array<com.aei.chatbot.domain.model.ProviderConfig>::class.java).toList() } catch (_: Exception) { emptyList() }
            )
            val u = update(c)
            p[UserPreferencesDataStore.KEY_SERVER_IP] = u.serverIp
            p[UserPreferencesDataStore.KEY_SERVER_PORT] = u.serverPort
            p[UserPreferencesDataStore.KEY_API_ENDPOINT] = u.apiEndpoint
            p[UserPreferencesDataStore.KEY_API_KEY] = u.apiKey
            p[UserPreferencesDataStore.KEY_SELECTED_MODEL] = u.selectedModel
            p[UserPreferencesDataStore.KEY_SYSTEM_PROMPT] = u.systemPrompt
            p[UserPreferencesDataStore.KEY_MAX_TOKENS] = u.maxTokens
            p[UserPreferencesDataStore.KEY_TEMPERATURE] = u.temperature
            p[UserPreferencesDataStore.KEY_STREAMING_ENABLED] = u.streamingEnabled
            p[UserPreferencesDataStore.KEY_TIMEOUT_SECONDS] = u.timeoutSeconds
            p[UserPreferencesDataStore.KEY_CONNECTION_MODE] = u.connectionMode
            p[UserPreferencesDataStore.KEY_REMOTE_URL] = u.remoteUrl
            p[UserPreferencesDataStore.KEY_WEB_SEARCH_ENABLED] = u.webSearchEnabled
            p[UserPreferencesDataStore.KEY_SEARXNG_URL] = u.searxngUrl
            p[UserPreferencesDataStore.KEY_WEB_SEARCH_RESULT_COUNT] = u.webSearchResultCount
            p[UserPreferencesDataStore.KEY_WEB_SEARCH_MODE] = u.webSearchMode
            p[UserPreferencesDataStore.KEY_APP_LANGUAGE] = u.appLanguage
            p[UserPreferencesDataStore.KEY_TRANSLATION_LANGUAGE] = u.translationLanguage
            p[UserPreferencesDataStore.KEY_VOICE_INPUT_LANGUAGE] = u.voiceInputLanguage
            p[UserPreferencesDataStore.KEY_AUTO_DETECT_LANGUAGE] = u.autoDetectLanguage
            p[UserPreferencesDataStore.KEY_THEME_MODE] = u.themeMode
            p[UserPreferencesDataStore.KEY_DYNAMIC_COLOR] = u.dynamicColor
            p[UserPreferencesDataStore.KEY_BUBBLE_STYLE] = u.bubbleStyle
            p[UserPreferencesDataStore.KEY_FONT_SIZE] = u.fontSize
            p[UserPreferencesDataStore.KEY_SHOW_TIMESTAMPS] = u.showTimestamps
            p[UserPreferencesDataStore.KEY_SHOW_AVATARS] = u.showAvatars
            p[UserPreferencesDataStore.KEY_USER_INITIALS] = u.userInitials
            p[UserPreferencesDataStore.KEY_AVATAR_COLOR] = u.avatarColor
            p[UserPreferencesDataStore.KEY_HAPTIC_FEEDBACK] = u.hapticFeedback
            p[UserPreferencesDataStore.KEY_SOUND_EFFECTS] = u.soundEffects
            p[UserPreferencesDataStore.KEY_AUTO_SCROLL] = u.autoScroll
            p[UserPreferencesDataStore.KEY_CLEAR_ON_NEW_SESSION] = u.clearOnNewSession
            p[UserPreferencesDataStore.KEY_IS_FIRST_LAUNCH] = u.isFirstLaunch
            p[UserPreferencesDataStore.KEY_ACTIVE_CHAT_ID] = u.activeChatId
            p[UserPreferencesDataStore.KEY_PROMPT_ENHANCEMENT_ENABLED] = u.promptEnhancementEnabled
            p[UserPreferencesDataStore.KEY_PROMPT_ENHANCEMENT_INSTRUCTION] = u.promptEnhancementInstruction
            p[UserPreferencesDataStore.KEY_ENHANCEMENT_MODEL] = u.enhancementModel
            p[UserPreferencesDataStore.KEY_PROVIDERS] = com.google.gson.Gson().toJson(u.providers)
            p[UserPreferencesDataStore.KEY_QUICK_MODELS] = com.google.gson.Gson().toJson(u.quickModels)
        }
    }

    override suspend fun resetToDefaults() { dataStore.resetToDefaults() }
}
