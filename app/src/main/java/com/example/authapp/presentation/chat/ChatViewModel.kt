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


    // =========================================================
    // Messages
    // =========================================================

    private val _messages =
        MutableStateFlow<List<Message>>(
            emptyList()
        )

    val messages =
        _messages.asStateFlow()


    // =========================================================
    // Inbox
    // =========================================================

    private val _inbox =
        MutableStateFlow<List<Chat>>(
            emptyList()
        )

    val inbox =
        _inbox.asStateFlow()


    // =========================================================
    // Active chat ID
    // =========================================================

    private val _chatId =
        MutableStateFlow("")

    val chatId =
        _chatId.asStateFlow()


    // =========================================================
    // Chat screen state
    // =========================================================

    private val _uiState =
        MutableStateFlow<ChatUiState>(
            ChatUiState.Idle
        )

    val uiState =
        _uiState.asStateFlow()


    // =========================================================
    // Unread
    // =========================================================

    private val _unreadCount =
        MutableStateFlow(0)

    val unreadCount =
        _unreadCount.asStateFlow()


    // =========================================================
    // One-time events
    // =========================================================

    private val _events =
        Channel<ChatEvent>(
            Channel.BUFFERED
        )

    val events =
        _events.receiveAsFlow()


    // =========================================================
    // Jobs
    // =========================================================

    private var messagesJob:
            Job? =
        null

    private var inboxJob:
            Job? =
        null

    private var unreadJob:
            Job? =
        null


    val myUid: String
        get() =
            authRepository
                .getCurrentUid()
                .orEmpty()


    // =========================================================
    // Unread count
    // =========================================================

    fun loadUnreadCount() {

        val uid =
            authRepository
                .getCurrentUid()


        if (
            uid.isNullOrBlank()
        ) {

            unreadJob?.cancel()

            _unreadCount.value =
                0

            return
        }


        unreadJob?.cancel()


        unreadJob =
            viewModelScope.launch {

                chatRepository
                    .getInbox(
                        uid
                    )
                    .catch {

                        _unreadCount.value =
                            0
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
            authRepository
                .getCurrentUid()
                ?: return


        if (
            chatId.isBlank()
        ) {

            return
        }


        viewModelScope.launch {

            chatRepository
                .markChatAsRead(
                    chatId,
                    uid
                )
        }
    }


    // =========================================================
    // Legacy Inbox support
    // =========================================================

    fun loadInbox() {

        val uid =
            authRepository
                .getCurrentUid()


        if (
            uid.isNullOrBlank()
        ) {

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
                    .getInbox(
                        uid
                    )
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


                                        if (
                                            result.isSuccess
                                        ) {

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
                                    otherUserName =
                                        name
                                )
                            }


                        _inbox.value =
                            enriched
                    }
            }
    }


    // =========================================================
    // Open / create chat
    // =========================================================

    fun openChat(
        otherUid: String
    ) {

        startOpening()


        val currentUid =
            authRepository
                .getCurrentUid()


        if (
            currentUid.isNullOrBlank()
        ) {

            showScreenError(
                "Your session has expired. Please sign in again."
            )

            return
        }


        if (
            otherUid.isBlank()
        ) {

            showScreenError(
                "The user for this conversation could not be found."
            )

            return
        }


        if (
            otherUid == currentUid
        ) {

            showScreenError(
                "You cannot start a conversation with yourself."
            )

            return
        }


        viewModelScope.launch {

            /*
             * First verify that the other user
             * still exists.
             */
            val otherUserResult =
                authRepository
                    .getUserFromFirestore(
                        otherUid
                    )


            if (
                otherUserResult.isFailure
            ) {

                showScreenError(
                    "This user is currently unavailable."
                )

                return@launch
            }


            /*
             * Repository safely returns an existing
             * deterministic chat or creates one.
             */
            val result =
                chatRepository
                    .getOrCreateChat(
                        currentUid,
                        otherUid
                    )


            if (
                result.isSuccess
            ) {

                activateChat(
                    result.getOrThrow()
                )

            } else {

                showScreenError(
                    result
                        .exceptionOrNull()
                        ?.message
                        ?: "Unable to open this conversation."
                )
            }
        }
    }


    // =========================================================
    // Open existing chat from Inbox
    // =========================================================

    fun initWithChatId(
        chatId: String
    ) {

        startOpening()


        val uid =
            authRepository
                .getCurrentUid()


        if (
            uid.isNullOrBlank()
        ) {

            showScreenError(
                "Your session has expired. Please sign in again."
            )

            return
        }


        if (
            chatId.isBlank()
        ) {

            showScreenError(
                "Conversation information is missing."
            )

            return
        }


        viewModelScope.launch {

            /*
             * Existing chat IDs must be validated
             * before listening to their messages.
             */
            val accessResult =
                chatRepository
                    .canAccessChat(
                        chatId,
                        uid
                    )


            val allowed =
                accessResult
                    .getOrElse {

                        false
                    }


            if (
                !allowed
            ) {

                showScreenError(
                    "You cannot access this conversation."
                )

                return@launch
            }


            activateChat(
                chatId
            )
        }
    }


    private fun startOpening() {

        /*
         * Cancel a previous listener if this is a retry.
         */
        messagesJob?.cancel()


        _messages.value =
            emptyList()


        _chatId.value =
            ""


        _uiState.value =
            ChatUiState.Opening
    }


    private fun activateChat(
        chatId: String
    ) {

        _chatId.value =
            chatId


        /*
         * IMPORTANT:
         *
         * We do NOT mark the UI as Ready here.
         *
         * The screen stays on "Opening conversation..."
         * until Firestore gives us its FIRST message snapshot.
         *
         * That snapshot may contain messages OR an actual
         * empty list. Only after that do we know the chat
         * successfully loaded.
         */
        listenToMessages(
            chatId
        )
    }


    // =========================================================
    // Realtime messages
    // =========================================================

    private fun listenToMessages(
        chatId: String
    ) {

        val uid =
            authRepository
                .getCurrentUid()


        if (
            uid.isNullOrBlank()
        ) {

            showScreenError(
                "Your session has expired. Please sign in again."
            )

            return
        }


        messagesJob?.cancel()


        messagesJob =
            viewModelScope.launch {

                chatRepository
                    .getMessages(
                        chatId
                    )
                    .catch {

                        /*
                         * A realtime listener failure is a
                         * genuine conversation-load error.
                         */
                        _messages.value =
                            emptyList()


                        _chatId.value =
                            ""


                        _uiState.value =
                            ChatUiState.Error(
                                "Unable to load this conversation."
                            )
                    }
                    .collect { messages ->

                        _messages.value =
                            messages


                        /*
                         * The first successful Firestore snapshot
                         * means conversation loading is complete.
                         *
                         * If the snapshot is empty, that is a valid
                         * empty conversation, NOT an error.
                         */
                        if (
                            _uiState.value
                                    !is ChatUiState.Ready
                        ) {

                            _uiState.value =
                                ChatUiState.Ready


                            _events.send(
                                ChatEvent.ChatReady(
                                    chatId
                                )
                            )
                        }


                        /*
                         * Keep unread state cleared when a new
                         * received message arrives while the
                         * conversation is already open.
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


    // =========================================================
    // Send message
    // =========================================================

    fun sendMessage(
        text: String
    ) {

        val cleanText =
            text.trim()


        if (
            cleanText.isBlank()
        ) {

            return
        }


        val uid =
            authRepository
                .getCurrentUid()


        if (
            uid.isNullOrBlank()
        ) {

            sendEventError(
                "Your session has expired."
            )

            return
        }


        val currentChatId =
            _chatId.value


        if (
            currentChatId.isBlank() ||
            _uiState.value
                    !is ChatUiState.Ready
        ) {

            sendEventError(
                "Conversation is still opening."
            )

            return
        }


        viewModelScope.launch {

            val result =
                chatRepository
                    .sendMessage(
                        chatId =
                            currentChatId,

                        senderId =
                            uid,

                        text =
                            cleanText
                    )


            if (
                result.isSuccess
            ) {

                _events.send(
                    ChatEvent.MessageSent
                )

            } else {

                _events.send(
                    ChatEvent.Error(
                        result
                            .exceptionOrNull()
                            ?.message
                            ?: "Message could not be sent."
                    )
                )
            }
        }
    }


    // =========================================================
    // Helpers
    // =========================================================

    private fun showScreenError(
        message: String
    ) {

        messagesJob?.cancel()


        _messages.value =
            emptyList()


        _chatId.value =
            ""


        _uiState.value =
            ChatUiState.Error(
                message
            )
    }


    private fun sendEventError(
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


    override fun onCleared() {
        super.onCleared()

        messagesJob?.cancel()

        inboxJob?.cancel()

        unreadJob?.cancel()
    }
}


// =============================================================
// Screen state
// =============================================================

sealed class ChatUiState {

    object Idle :
        ChatUiState()


    object Opening :
        ChatUiState()


    object Ready :
        ChatUiState()


    data class Error(
        val message: String
    ) : ChatUiState()
}


// =============================================================
// One-time events
// =============================================================

sealed class ChatEvent {

    data class ChatReady(
        val chatId: String
    ) : ChatEvent()


    object MessageSent :
        ChatEvent()


    data class Error(
        val message: String
    ) : ChatEvent()
}