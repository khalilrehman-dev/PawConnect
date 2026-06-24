package com.example.authapp.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.model.Chat
import com.example.authapp.model.Message
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _inbox = MutableStateFlow<List<Chat>>(emptyList())
    val inbox = _inbox.asStateFlow()

    private val _chatId = MutableStateFlow("")
    val chatId = _chatId.asStateFlow()

    private val _events = Channel<ChatEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val myUid: String get() = authRepository.getCurrentUid() ?: ""

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

    fun loadUnreadCount() {
        val uid = authRepository.getCurrentUid() ?: return
        viewModelScope.launch {
            chatRepository.getInbox(uid).collect { chats ->
                // Count chats where last message was not sent by me
                _unreadCount.value = chats.count { chat ->
                    chat.unreadBy.contains(uid)
                }
            }
        }
    }

    fun markChatAsRead(chatId: String) {
        val uid = authRepository.getCurrentUid() ?: return
        viewModelScope.launch {
            chatRepository.markChatAsRead(chatId, uid)
        }
    }

    // ── Inbox ─────────────────────────────────────────────────────────────────

    fun loadInbox() {
        val uid = authRepository.getCurrentUid() ?: return
        viewModelScope.launch {
            chatRepository.getInbox(uid).collect { chats ->
                val enriched = chats.map { chat ->
                    val name = try {
                        val result = authRepository.getUserFromFirestore(chat.otherUserId)
                        if (result.isSuccess) result.getOrThrow().displayName
                        else "User"
                    } catch (e: Exception) {
                        "User"
                    }

                    chat.copy(otherUserName = name)
                }
                _inbox.value = enriched
            }
        }
    }

    // ── Open or create chat ───────────────────────────────────────────────────

    fun openChat(otherUid: String) {
        val myUid = authRepository.getCurrentUid() ?: return
        viewModelScope.launch {
            val result = chatRepository.getOrCreateChat(myUid, otherUid)
            if (result.isSuccess) {
                val id = result.getOrThrow()
                _chatId.value = id
                listenToMessages(id)
                _events.send(ChatEvent.ChatReady(id))
            } else {
                _events.send(ChatEvent.Error("Failed to open chat"))
            }
        }
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    fun listenToMessages(chatId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val uid    = authRepository.getCurrentUid() ?: return
        val chatId = _chatId.value
        if (chatId.isEmpty()) return

        viewModelScope.launch {
            chatRepository.sendMessage(chatId, uid, text)
        }
    }

    // ── For when ChatActivity is opened directly with chatId ──────────────────

    fun initWithChatId(chatId: String) {
        _chatId.value = chatId
        listenToMessages(chatId)
    }
}

sealed class ChatEvent {
    data class ChatReady(val chatId: String) : ChatEvent()
    data class Error(val message: String) : ChatEvent()
}