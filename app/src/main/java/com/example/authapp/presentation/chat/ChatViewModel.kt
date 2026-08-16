package com.example.authapp.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.domain.repository.ChatRepository
import com.example.authapp.model.Chat
import com.example.authapp.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _messages =
        MutableStateFlow<List<Message>>(
            emptyList()
        )

    val messages =
        _messages.asStateFlow()


    private val _inbox =
        MutableStateFlow<List<Chat>>(
            emptyList()
        )

    val inbox =
        _inbox.asStateFlow()


    private val _chatId =
        MutableStateFlow("")

    val chatId =
        _chatId.asStateFlow()


    private val _unreadCount =
        MutableStateFlow(0)

    val unreadCount =
        _unreadCount.asStateFlow()


    private val _events =
        Channel<ChatEvent>(
            Channel.BUFFERED
        )

    val events =
        _events.receiveAsFlow()


    private var messagesJob: Job? = null
    private var inboxJob: Job? = null
    private var unreadJob: Job? = null


    val myUid: String
        get() =
            authRepository.getCurrentUid()
                ?: ""


    // ─────────────────────────────────────
    // Unread count
    // ─────────────────────────────────────

    fun loadUnreadCount() {

        val uid =
            authRepository.getCurrentUid()

        if (uid.isNullOrBlank()) {

            _unreadCount.value = 0
            return
        }

        unreadJob?.cancel()

        unreadJob =
            viewModelScope.launch {

                chatRepository
                    .getInbox(uid)
                    .catch {

                        _unreadCount.value = 0
                    }
                    .collect { chats ->

                        _unreadCount.value =
                            chats.count { chat ->

                                uid in chat.unreadBy
                            }
                    }
            }
    }


    fun markChatAsRead(
        chatId: String
    ) {

        val uid =
            authRepository.getCurrentUid()
                ?: return

        if (chatId.isBlank()) {
            return
        }

        viewModelScope.launch {

            chatRepository.markChatAsRead(
                chatId,
                uid
            )
        }
    }


    // ─────────────────────────────────────
    // Inbox
    // ─────────────────────────────────────

    fun loadInbox() {

        val uid =
            authRepository.getCurrentUid()

        if (uid.isNullOrBlank()) {

            viewModelScope.launch {

                _events.send(
                    ChatEvent.Error(
                        "Session expired"
                    )
                )
            }

            return
        }

        inboxJob?.cancel()

        inboxJob =
            viewModelScope.launch {

                chatRepository
                    .getInbox(uid)
                    .catch {

                        _events.send(
                            ChatEvent.Error(
                                "Unable to load messages"
                            )
                        )
                    }
                    .collect { chats ->

                        val enriched =
                            chats.map { chat ->

                                val name =
                                    try {

                                        val result =
                                            authRepository
                                                .getUserFromFirestore(
                                                    chat.otherUserId
                                                )

                                        if (result.isSuccess) {

                                            result
                                                .getOrThrow()
                                                .displayName
                                                .ifBlank {
                                                    "User"
                                                }

                                        } else {

                                            "User"
                                        }

                                    } catch (
                                        exception: Exception
                                    ) {

                                        "User"
                                    }

                                chat.copy(
                                    otherUserName = name
                                )
                            }

                        _inbox.value =
                            enriched
                    }
            }
    }


    // ─────────────────────────────────────
    // Open / create chat
    // ─────────────────────────────────────

    fun openChat(
        otherUid: String
    ) {

        val currentUid =
            authRepository.getCurrentUid()

        if (currentUid.isNullOrBlank()) {

            sendError(
                "Session expired"
            )

            return
        }

        if (otherUid.isBlank()) {

            sendError(
                "User not found"
            )

            return
        }

        if (otherUid == currentUid) {

            sendError(
                "You cannot message yourself"
            )

            return
        }

        viewModelScope.launch {

            /*
             * Ensure target user actually exists.
             */
            val otherUserResult =
                authRepository
                    .getUserFromFirestore(
                        otherUid
                    )

            if (otherUserResult.isFailure) {

                _events.send(
                    ChatEvent.Error(
                        "This user is unavailable"
                    )
                )

                return@launch
            }

            val result =
                chatRepository
                    .getOrCreateChat(
                        currentUid,
                        otherUid
                    )

            if (result.isSuccess) {

                activateChat(
                    result.getOrThrow()
                )

            } else {

                _events.send(
                    ChatEvent.Error(
                        result.exceptionOrNull()
                            ?.message
                            ?: "Failed to open chat"
                    )
                )
            }
        }
    }


    // ─────────────────────────────────────
    // Open existing chat from Inbox
    // ─────────────────────────────────────

    fun initWithChatId(
        chatId: String
    ) {

        val uid =
            authRepository.getCurrentUid()

        if (uid.isNullOrBlank()) {

            sendError(
                "Session expired"
            )

            return
        }

        if (chatId.isBlank()) {

            sendError(
                "Chat not found"
            )

            return
        }

        viewModelScope.launch {

            val accessResult =
                chatRepository.canAccessChat(
                    chatId,
                    uid
                )

            val allowed =
                accessResult.getOrElse {
                    false
                }

            if (!allowed) {

                _events.send(
                    ChatEvent.Error(
                        "You cannot access this chat"
                    )
                )

                return@launch
            }

            activateChat(
                chatId
            )
        }
    }


    private suspend fun activateChat(
        chatId: String
    ) {

        _chatId.value =
            chatId

        listenToMessages(
            chatId
        )

        _events.send(
            ChatEvent.ChatReady(
                chatId
            )
        )
    }


    // ─────────────────────────────────────
    // Realtime messages
    // ─────────────────────────────────────

    private fun listenToMessages(
        chatId: String
    ) {

        val uid =
            authRepository.getCurrentUid()
                ?: return

        messagesJob?.cancel()

        messagesJob =
            viewModelScope.launch {

                chatRepository
                    .getMessages(chatId)
                    .catch {

                        _messages.value =
                            emptyList()

                        _chatId.value =
                            ""

                        _events.send(
                            ChatEvent.Error(
                                "Unable to load this chat"
                            )
                        )
                    }
                    .collect { messages ->

                        _messages.value =
                            messages

                        /*
                         * If a received message arrives while
                         * user is already viewing the chat,
                         * keep unread state cleared.
                         */
                        val latestMessage =
                            messages.lastOrNull()

                        if (
                            latestMessage != null &&
                            latestMessage.senderId != uid
                        ) {

                            chatRepository
                                .markChatAsRead(
                                    chatId,
                                    uid
                                )
                        }
                    }
            }
    }


    // ─────────────────────────────────────
    // Send
    // ─────────────────────────────────────

    fun sendMessage(
        text: String
    ) {

        val cleanText =
            text.trim()

        if (cleanText.isBlank()) {
            return
        }

        val uid =
            authRepository.getCurrentUid()

        if (uid.isNullOrBlank()) {

            sendError(
                "Session expired"
            )

            return
        }

        val currentChatId =
            _chatId.value

        if (currentChatId.isBlank()) {

            sendError(
                "Chat is not ready yet"
            )

            return
        }

        viewModelScope.launch {

            val result =
                chatRepository.sendMessage(
                    chatId =
                        currentChatId,

                    senderId =
                        uid,

                    text =
                        cleanText
                )

            if (result.isSuccess) {

                _events.send(
                    ChatEvent.MessageSent
                )

            } else {

                _events.send(
                    ChatEvent.Error(
                        result.exceptionOrNull()
                            ?.message
                            ?: "Message could not be sent"
                    )
                )
            }
        }
    }


    private fun sendError(
        message: String
    ) {

        viewModelScope.launch {

            _events.send(
                ChatEvent.Error(
                    message
                )
            )
        }
    }
}


sealed class ChatEvent {

    data class ChatReady(
        val chatId: String
    ) : ChatEvent()

    object MessageSent : ChatEvent()

    data class Error(
        val message: String
    ) : ChatEvent()
}