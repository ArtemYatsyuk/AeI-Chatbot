package com.aei.chatbot.di

import android.content.Context
import androidx.room.Room
import com.aei.chatbot.data.local.dao.ChatDao
import com.aei.chatbot.data.local.dao.MessageDao
import com.aei.chatbot.data.local.database.AeIDatabase
import com.aei.chatbot.data.local.preferences.UserPreferencesDataStore
import com.aei.chatbot.data.remote.search.WebSearchService
import com.aei.chatbot.data.repository.ChatRepository
import com.aei.chatbot.data.repository.ChatRepositoryImpl
import com.aei.chatbot.data.repository.SettingsRepository
import com.aei.chatbot.data.repository.SettingsRepositoryImpl
import com.aei.chatbot.util.Constants
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchModule {
    @Provides
    @Singleton
    fun provideWebSearchService(): WebSearchService = WebSearchService()
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AeIDatabase =
        Room.databaseBuilder(context, AeIDatabase::class.java, Constants.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideChatDao(db: AeIDatabase): ChatDao = db.chatDao()

    @Provides
    fun provideMessageDao(db: AeIDatabase): MessageDao = db.messageDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
