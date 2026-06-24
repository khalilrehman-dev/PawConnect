package com.example.authapp.model

data class Chat(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSenderId: String = "",
    val otherUserName: String = "",
    val otherUserId: String = "",
    val unreadBy: List<String> = emptyList()   // NEW
)