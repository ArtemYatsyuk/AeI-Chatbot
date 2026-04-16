package com.aei.chatbot.data.remote.network

import com.aei.chatbot.BuildConfig
import com.aei.chatbot.data.remote.api.LmStudioApiService
import com.google.gson.Gson
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
        .let { if (!it.startsWith("http")) "https://$it" else it }

    private val cleanEndpoint = apiEndpoint.trim().trimStart('/')
        .ifBlank { "v1/chat/completions" }

    private val baseUrl: String = when (connectionMode) {
        "ngrok", "cloud" -> "$cleanRemoteUrl/"
        else -> "http://$serverIp:$serverPort/"
    }

    val chatCompletionUrl: String = when (connectionMode) {
        "ngrok", "cloud" -> "$cleanRemoteUrl/$cleanEndpoint"
        else -> "http://$serverIp:$serverPort/$cleanEndpoint"
    }

    val modelsUrl: String = when (connectionMode) {
        "ngrok", "cloud" -> {
            val parts = cleanEndpoint.split("/")
            val prefix = if (parts.size >= 2) parts[0] else "v1"
            "$cleanRemoteUrl/$prefix/models"
        }
        else -> {
            val parts = cleanEndpoint.split("/")
            val prefix = if (parts.size >= 2) parts[0] else "v1"
            "http://$serverIp:$serverPort/$prefix/models"
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG_BUILD) HttpLoggingInterceptor.Level.HEADERS
        else HttpLoggingInterceptor.Level.NONE
    }

    private val authInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        if (apiKey.isNotBlank()) builder.addHeader("Authorization", "Bearer $apiKey")
        if (isNgrok) builder.addHeader("ngrok-skip-browser-warning", "true")
        // Cloud APIs use standard JSON accept
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
        .addConverterFactory(GsonConverterFactory.create(Gson()))
        .build()
        .create(LmStudioApiService::class.java)
}
