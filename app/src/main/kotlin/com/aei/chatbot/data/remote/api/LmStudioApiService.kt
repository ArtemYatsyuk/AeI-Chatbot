package com.aei.chatbot.data.remote.api

import com.aei.chatbot.data.remote.model.ChatRequest
import com.aei.chatbot.data.remote.model.ChatResponse
import com.aei.chatbot.data.remote.model.ModelsResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

interface LmStudioApiService {
    @GET
    suspend fun getModels(@Url url: String): Response<ModelsResponse>

    @Streaming
    @POST
    suspend fun streamChatCompletion(@Url url: String, @Body request: ChatRequest): Response<ResponseBody>

    @POST
    suspend fun chatCompletion(@Url url: String, @Body request: ChatRequest): Response<ChatResponse>
}
