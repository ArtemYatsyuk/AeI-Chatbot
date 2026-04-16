package com.aei.chatbot.data.local.dao

import androidx.room.*
import com.aei.chatbot.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()

    @Query("SELECT * FROM chats WHERE name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchChats(query: String): Flow<List<ChatEntity>>
}
