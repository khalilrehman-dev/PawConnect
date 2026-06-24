package com.example.authapp.domain.repository

import com.example.authapp.model.Chat
import com.example.authapp.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    /** Get or create a chat between two users — returns chatId */
    suspend fun getOrCreateChat(myUid: String, otherUid: String): Result<String>

    /** Real-time stream of messages in a chat */
    fun getMessages(chatId: String): Flow<List<Message>>

    /** Send a message */
    suspend fun sendMessage(chatId: String, senderId: String, text: String): Result<Unit>

    /** Get all chats for a user — real-time */
    fun getInbox(uid: String): Flow<List<Chat>>

    suspend fun markChatAsRead(chatId: String, userId: String)

}