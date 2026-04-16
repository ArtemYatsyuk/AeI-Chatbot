package com.aei.chatbot

import com.aei.chatbot.domain.model.ApiResult
import com.aei.chatbot.domain.model.AppSettings
import com.aei.chatbot.domain.model.ChatMessage
import com.aei.chatbot.domain.usecase.SendMessageUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatRepositoryTest {
    private val sendMessageUseCase = mockk<SendMessageUseCase>()

    @Test
    fun `stream emits success tokens`() = runTest {
        val settings = AppSettings()
        val messages = listOf(
            ChatMessage("1", "chat1", "user", "Hello", null, System.currentTimeMillis())
        )
        coEvery { sendMessageUseCase.stream(settings, messages) } returns flowOf(
            ApiResult.Loading,
            ApiResult.Success("Hello"),
            ApiResult.Success(" world")
        )

        val results = mutableListOf<ApiResult<String>>()
        sendMessageUseCase.stream(settings, messages).collect { results.add(it) }
        assertEquals(3, results.size)
        assertEquals("Hello", (results[1] as ApiResult.Success).data)
    }

    @Test
    fun `send returns error on failure`() = runTest {
        val settings = AppSettings()
        val messages = emptyList<ChatMessage>()
        coEvery { sendMessageUseCase.send(settings, messages) } returns ApiResult.Error("Connection refused.")
        val result = sendMessageUseCase.send(settings, messages)
        assert(result is ApiResult.Error)
        assertEquals("Connection refused.", (result as ApiResult.Error).message)
    }
}
