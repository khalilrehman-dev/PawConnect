package com.example.authapp.domain.repository

import com.example.authapp.model.Chat
import com.example.authapp.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    suspend fun getOrCreateChat(
        myUid: String,
        otherUid: String
    ): Result<String>

    suspend fun canAccessChat(
        chatId: String,
        userId: String
    ): Result<Boolean>

    fun getMessages(
        chatId: String
    ): Flow<List<Message>>

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        text: String
    ): Result<Unit>

    fun getInbox(
        uid: String
    ): Flow<List<Chat>>

    suspend fun markChatAsRead(
        chatId: String,
        userId: String
    ): Result<Unit>
}