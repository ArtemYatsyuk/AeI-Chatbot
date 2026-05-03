package com.aei.chatbot.data.remote.network

import com.aei.chatbot.BuildConfig
import com.aei.chatbot.data.remote.api.LmStudioApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.aei.chatbot.data.remote.model.MessageSerializer
import com.aei.chatbot.data.remote.model.Message
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient(
    serverIp: String,
    serverPort: Int,
    timeoutSeconds: Int,
    apiKey: String,
    val connectionMode: String = "local",
    remoteUrl: String = "",
    apiEndpoint: String = "v1/chat/completions"
) {
    private val isNgrok = connectionMode == "ngrok" && remoteUrl.contains("ngrok", ignoreCase = true)

    private val cleanRemoteUrl = remoteUrl.trim().trimEnd('/')
        .let { if (it.isNotBlank() && !it.startsWith("http")) "https://$it" else it }

    private val cleanEndpoint = apiEndpoint.trim().trimStart('/')
        .ifBlank { "v1/chat/completions" }

    // If remoteUrl already ends with the endpoint path, don't append again
    private fun buildChatUrl(base: String): String {
        val ep = cleanEndpoint
        return if (base.endsWith(ep) || base.endsWith(ep.trimStart('/'))) base
        else "$base/$ep"
    }

    private val localBase = "http://$serverIp:$serverPort"

    private val baseUrl: String = when (connectionMode) {
        "ngrok", "cloud" -> if (cleanRemoteUrl.isNotBlank()) "$cleanRemoteUrl/" else "$localBase/"
        else -> "$localBase/"
    }

    val chatCompletionUrl: String = when (connectionMode) {
        "ngrok", "cloud" -> if (cleanRemoteUrl.isNotBlank()) buildChatUrl(cleanRemoteUrl) else buildChatUrl(localBase)
        else -> buildChatUrl(localBase)
    }

    val modelsUrl: String = run {
        val versionPrefix = cleanEndpoint.split("/").firstOrNull()?.takeIf { it.matches(Regex("""v\d+""")) } ?: "v1"
        val base = when (connectionMode) {
            "ngrok", "cloud" -> if (cleanRemoteUrl.isNotBlank()) cleanRemoteUrl else localBase
            else -> localBase
        }
        "$base/$versionPrefix/models"
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG_BUILD) HttpLoggingInterceptor.Level.HEADERS
        else HttpLoggingInterceptor.Level.NONE
    }

    private val authInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        if (apiKey.isNotBlank()) builder.addHeader("Authorization", "Bearer $apiKey")
        if (isNgrok) builder.addHeader("ngrok-skip-browser-warning", "true")
        chain.proceed(builder.build())
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: LmStudioApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(
            GsonBuilder()
                .setLenient()
                .registerTypeAdapter(Message::class.java, MessageSerializer())
                .create()
        ))
        .build()
        .create(LmStudioApiService::class.java)
}