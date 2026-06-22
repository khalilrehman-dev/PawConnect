package com.example.authapp.model


data class Chat(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSenderId: String = "",
    val otherUserName: String = "",     // populated client-side for display
    val otherUserId: String = ""        // populated client-side for navigation
)