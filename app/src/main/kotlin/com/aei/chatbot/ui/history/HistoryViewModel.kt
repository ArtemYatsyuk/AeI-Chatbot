package com.aei.chatbot.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aei.chatbot.domain.model.ChatSession
import com.aei.chatbot.domain.usecase.DeleteChatUseCase
import com.aei.chatbot.domain.usecase.GetChatHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val deleteChatUseCase: DeleteChatUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val chats: StateFlow<List<ChatSession>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) getChatHistoryUseCase.allChats()
            else getChatHistoryUseCase.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deletedChat = MutableSharedFlow<ChatSession>()
    val deletedChat: SharedFlow<ChatSession> = _deletedChat.asSharedFlow()

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            val chat = chats.value.find { it.id == chatId }
            deleteChatUseCase.deleteChat(chatId)
            chat?.let { _deletedChat.emit(it) }
        }
    }

    fun restoreChat(chat: ChatSession) {
        // Restoration would require re-inserting the chat — for simplicity we navigate back
    }
}
