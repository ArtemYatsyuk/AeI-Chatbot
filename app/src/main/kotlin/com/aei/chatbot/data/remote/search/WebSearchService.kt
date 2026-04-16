package com.aei.chatbot.data.remote.search

import com.aei.chatbot.domain.model.AppSettings
import com.aei.chatbot.domain.model.WebSearchResult
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class SearxngResponse(
    @SerializedName("results") val results: List<SearxngResult>?
)

data class SearxngResult(
    @SerializedName("title") val title: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("content") val content: String?
)

@Singleton
class WebSearchService @Inject constructor() {
    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun search(query: String, settings: AppSettings): List<WebSearchResult> =
        withContext(Dispatchers.IO) {
            try {
                searchSearxng(query, settings)
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun searchSearxng(query: String, settings: AppSettings): List<WebSearchResult> {
        val baseUrl = settings.searxngUrl.trimEnd('/')
        if (baseUrl.isBlank()) return emptyList()

        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/search?q=$encoded&format=json&categories=general&language=auto&pageno=1"

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .addHeader("Accept", "application/json")
            .addHeader("ngrok-skip-browser-warning", "true")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()
        val body = response.body?.string() ?: return emptyList()

        val parsed = try {
            gson.fromJson(body, SearxngResponse::class.java)
        } catch (_: Exception) {
            return emptyList()
        }

        return parsed.results
            ?.take(settings.webSearchResultCount)
            ?.mapNotNull { r ->
                val title = r.title ?: return@mapNotNull null
                val resultUrl = r.url ?: return@mapNotNull null
                val snippet = r.content ?: ""
                WebSearchResult(title = title, url = resultUrl, snippet = snippet.take(300))
            } ?: emptyList()
    }

    fun formatResultsForPrompt(results: List<WebSearchResult>): String {
        if (results.isEmpty()) return ""
        val sb = StringBuilder()
        sb.appendLine("[Web Search Results]")
        results.forEachIndexed { i, r ->
            sb.appendLine("${i + 1}. ${r.title}")
            sb.appendLine("   URL: ${r.url}")
            sb.appendLine("   ${r.snippet}")
        }
        sb.appendLine("[End of Web Search Results]")
        sb.appendLine("Use the above search results to answer the user's question. Cite sources when possible.")
        return sb.toString()
    }
}
