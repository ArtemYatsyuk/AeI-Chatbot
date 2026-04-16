package com.aei.chatbot.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aei.chatbot.data.local.dao.ChatDao
import com.aei.chatbot.data.local.dao.MessageDao
import com.aei.chatbot.data.local.entity.ChatEntity
import com.aei.chatbot.data.local.entity.MessageEntity

@Database(
    entities = [ChatEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AeIDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
}
