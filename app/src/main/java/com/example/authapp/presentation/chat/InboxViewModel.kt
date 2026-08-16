package com.example.authapp.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class InboxViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<InboxUiState>(
            InboxUiState.Loading
        )

    val uiState =
        _uiState.asStateFlow()


    private var inboxJob:
            Job? =
        null


    fun loadInbox() {

        val myUid =
            authRepository
                .getCurrentUid()


        if (
            myUid.isNullOrBlank()
        ) {

            inboxJob?.cancel()


            _uiState.value =
                InboxUiState.Error(
                    "Your session has expired. Please sign in again."
                )


            return
        }


        inboxJob?.cancel()


        inboxJob =
            viewModelScope.launch {

                _uiState.value =
                    InboxUiState.Loading


                chatRepository
                    .getInbox(
                        myUid
                    )
                    .catch { error ->

                        _uiState.value =
                            InboxUiState.Error(
                                error.message
                                    ?: "Unable to load conversations."
                            )
                    }
                    .collect { chats ->

                        if (
                            chats.isEmpty()
                        ) {

                            _uiState.value =
                                InboxUiState.Empty

                            return@collect
                        }


                        val conversations =
                            chats.map { chat ->

                                /*
                                 * ChatRepository already gives us
                                 * the other participant's UID.
                                 *
                                 * Resolve their profile so Inbox
                                 * can show their real display name
                                 * and profile image.
                                 */
                                val otherUser =
                                    if (
                                        chat.otherUserId
                                            .isBlank()
                                    ) {

                                        null

                                    } else {

                                        authRepository
                                            .getUserFromFirestore(
                                                chat.otherUserId
                                            )
                                            .getOrNull()
                                    }


                                val displayName =
                                    otherUser
                                        ?.displayName
                                        ?.trim()
                                        .orEmpty()
                                        .ifBlank {

                                            chat.otherUserName
                                                .trim()
                                                .ifBlank {

                                                    "User"
                                                }
                                        }


                                val preview =
                                    if (
                                        chat.lastMessageSenderId ==
                                        myUid
                                    ) {

                                        "You: ${chat.lastMessage}"

                                    } else {

                                        chat.lastMessage
                                    }


                                InboxUiItem(
                                    chatId =
                                        chat.chatId,

                                    otherUserId =
                                        chat.otherUserId,

                                    otherUserName =
                                        displayName,

                                    profileImageUrl =
                                        otherUser
                                            ?.profileImageUrl
                                            .orEmpty(),

                                    lastMessage =
                                        preview,

                                    lastMessageTime =
                                        chat.lastMessageTime,

                                    isUnread =
                                        myUid in
                                                chat.unreadBy
                                )
                            }


                        _uiState.value =
                            InboxUiState.Success(
                                conversations =
                                    conversations
                            )
                    }
            }
    }


    override fun onCleared() {
        super.onCleared()

        inboxJob?.cancel()
    }
}


sealed class InboxUiState {

    object Loading :
        InboxUiState()


    data class Success(
        val conversations:
        List<InboxUiItem>
    ) : InboxUiState()


    object Empty :
        InboxUiState()


    data class Error(
        val message: String
    ) : InboxUiState()
}


data class InboxUiItem(

    val chatId: String,

    val otherUserId: String,

    val otherUserName: String,

    val profileImageUrl: String,

    val lastMessage: String,

    val lastMessageTime: Long,

    val isUnread: Boolean
)