package com.aei.chatbot.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class TranslationManager @Inject constructor() {
    private val translators = HashMap<String, Translator>()
    private val _downloadProgress = MutableStateFlow<Pair<String, Float>?>(null)
    val downloadProgress: Flow<Pair<String, Float>?> = _downloadProgress.asStateFlow()

    fun langCodeToTranslateLanguage(code: String): String = when (code) {
        "en-US", "en-GB" -> TranslateLanguage.ENGLISH
        "uk" -> TranslateLanguage.UKRAINIAN
        "cs" -> TranslateLanguage.CZECH
        "zh-CN" -> TranslateLanguage.CHINESE
        else -> TranslateLanguage.ENGLISH
    }

    private fun getOrCreateTranslator(sourceLang: String, targetLang: String): Translator {
        val key = "$sourceLang->$targetLang"
        return translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            Translation.getClient(options)
        }
    }

    suspend fun translate(text: String, targetLanguageCode: String): String {
        if (text.isBlank()) return text
        val targetLang = langCodeToTranslateLanguage(targetLanguageCode)
        val translator = getOrCreateTranslator(TranslateLanguage.ENGLISH, targetLang)
        return try {
            ensureModelDownloaded(translator, targetLanguageCode)
            suspendCancellableCoroutine { cont ->
                translator.translate(text)
                    .addOnSuccessListener { translated -> cont.resume(translated) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }
        } catch (e: Exception) {
            "$text\n[Translation unavailable]"
        }
    }

    private suspend fun ensureModelDownloaded(translator: Translator, langCode: String) {
        suspendCancellableCoroutine { cont ->
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    _downloadProgress.value = null
                    cont.resume(Unit)
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    fun closeTranslator(langCode: String) {
        val targetLang = langCodeToTranslateLanguage(langCode)
        val key = "${TranslateLanguage.ENGLISH}->$targetLang"
        translators[key]?.close()
        translators.remove(key)
    }

    fun closeAll() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
}
