package com.example.authapp.data.repository

import com.example.authapp.model.Chat
import com.example.authapp.model.Message
import com.example.authapp.domain.repository.ChatRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    companion object {
        private const val CHATS    = "chats"
        private const val MESSAGES = "messages"
    }

    private fun getChatId(uid1: String, uid2: String): String =
        if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"

    override suspend fun getOrCreateChat(
        myUid: String,
        otherUid: String
    ): Result<String> = runCatching {

        require(myUid.isNotBlank()) {
            "Your session is invalid"
        }

        require(otherUid.isNotBlank()) {
            "User not found"
        }

        require(myUid != otherUid) {
            "You cannot start a chat with yourself"
        }

        val chatId =
            getChatId(myUid, otherUid)

        val participants =
            listOf(myUid, otherUid).sorted()

        val chatRef =
            firestore.collection(CHATS)
                .document(chatId)

        firestore.runTransaction { transaction ->

            val snapshot =
                transaction.get(chatRef)

            if (!snapshot.exists()) {

                transaction.set(
                    chatRef,
                    mapOf(
                        "chatId" to chatId,
                        "participants" to participants,
                        "lastMessage" to "",
                        "lastMessageTime" to 0L,
                        "lastMessageSenderId" to "",
                        "unreadBy" to emptyList<String>()
                    )
                )

            } else {

                val existingParticipants =
                    (snapshot.get("participants") as? List<*>)
                        ?.mapNotNull { it?.toString() }
                        ?.distinct()
                        ?.sorted()
                        ?: emptyList()

                if (existingParticipants != participants) {
                    error("Existing chat data is invalid")
                }
            }

            chatId
        }.await()
    }

    override suspend fun canAccessChat(
        chatId: String,
        userId: String
    ): Result<Boolean> = runCatching {

        if (
            chatId.isBlank() ||
            userId.isBlank()
        ) {
            return@runCatching false
        }

        val snapshot =
            firestore.collection(CHATS)
                .document(chatId)
                .get()
                .await()

        if (!snapshot.exists()) {
            return@runCatching false
        }

        val participants =
            (snapshot.get("participants") as? List<*>)
                ?.mapNotNull { it?.toString() }
                ?.distinct()
                ?: emptyList()

        participants.size == 2 &&
                userId in participants
    }

    override fun getMessages(
        chatId: String
    ): Flow<List<Message>> = callbackFlow {

        val listener =
            firestore
                .collection(MESSAGES)
                .document(chatId)
                .collection(MESSAGES)
                .orderBy(
                    "timestamp",
                    Query.Direction.ASCENDING
                )
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val messages =
                        snapshot?.documents
                            ?.mapNotNull { doc ->

                                val senderId =
                                    doc.getString("senderId")
                                        ?: return@mapNotNull null

                                val text =
                                    doc.getString("text")
                                        ?.trim()
                                        .orEmpty()

                                if (
                                    senderId.isBlank() ||
                                    text.isBlank()
                                ) {
                                    return@mapNotNull null
                                }

                                Message(
                                    id =
                                        doc.getString("id")
                                            ?: doc.id,

                                    chatId =
                                        doc.getString("chatId")
                                            ?: chatId,

                                    senderId =
                                        senderId,

                                    text =
                                        text,

                                    timestamp =
                                        doc.getLong("timestamp")
                                            ?: 0L
                                )
                            }
                            ?: emptyList()

                    trySend(messages)
                }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun sendMessage(
        chatId: String,
        senderId: String,
        text: String
    ): Result<Unit> = runCatching {

        val cleanText =
            text.trim()

        require(chatId.isNotBlank()) {
            "Chat is unavailable"
        }

        require(senderId.isNotBlank()) {
            "Your session is invalid"
        }

        require(cleanText.isNotBlank()) {
            "Message cannot be empty"
        }

        val chatRef =
            firestore.collection(CHATS)
                .document(chatId)

        val messageRef =
            firestore.collection(MESSAGES)
                .document(chatId)
                .collection(MESSAGES)
                .document()

        val timestamp =
            System.currentTimeMillis()

        firestore.runTransaction { transaction ->

            val chatSnapshot =
                transaction.get(chatRef)

            if (!chatSnapshot.exists()) {
                error("Chat no longer exists")
            }

            val participants =
                (chatSnapshot.get("participants") as? List<*>)
                    ?.mapNotNull { it?.toString() }
                    ?.distinct()
                    ?: emptyList()

            if (
                participants.size != 2 ||
                senderId !in participants
            ) {
                error(
                    "You are not allowed to send messages in this chat"
                )
            }

            val receiverId =
                participants.firstOrNull {
                    it != senderId
                } ?: error(
                    "Message receiver is invalid"
                )

            val message =
                mapOf(
                    "id" to messageRef.id,
                    "chatId" to chatId,
                    "senderId" to senderId,
                    "text" to cleanText,
                    "timestamp" to timestamp
                )

            transaction.set(
                messageRef,
                message
            )

            transaction.update(
                chatRef,
                mapOf(
                    "lastMessage" to cleanText,
                    "lastMessageTime" to timestamp,
                    "lastMessageSenderId" to senderId,
                    "unreadBy" to listOf(receiverId)
                )
            )

            Unit
        }.await()
    }

    override fun getInbox(
        uid: String
    ): Flow<List<Chat>> = callbackFlow {

        val listener =
            firestore.collection(CHATS)
                .whereArrayContains(
                    "participants",
                    uid
                )
                .orderBy(
                    "lastMessageTime",
                    Query.Direction.DESCENDING
                )
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val chats =
                        snapshot?.documents
                            ?.mapNotNull { doc ->

                                val participants =
                                    (doc.get("participants") as? List<*>)
                                        ?.mapNotNull {
                                            it?.toString()
                                        }
                                        ?.distinct()
                                        ?: return@mapNotNull null

                                if (
                                    participants.size != 2 ||
                                    uid !in participants
                                ) {
                                    return@mapNotNull null
                                }

                                val otherUid =
                                    participants.firstOrNull {
                                        it != uid
                                    } ?: return@mapNotNull null

                                val lastMessage =
                                    doc.getString(
                                        "lastMessage"
                                    ).orEmpty()

                                val lastMessageTime =
                                    doc.getLong(
                                        "lastMessageTime"
                                    ) ?: 0L

                                /*
                                 * Don't show a conversation
                                 * until at least one message
                                 * has actually been sent.
                                 */
                                if (
                                    lastMessage.isBlank() ||
                                    lastMessageTime <= 0L
                                ) {
                                    return@mapNotNull null
                                }

                                val unreadBy =
                                    (doc.get("unreadBy") as? List<*>)
                                        ?.mapNotNull {
                                            it?.toString()
                                        }
                                        ?: emptyList()

                                Chat(
                                    chatId =
                                        doc.getString("chatId")
                                            ?.takeIf {
                                                it.isNotBlank()
                                            }
                                            ?: doc.id,

                                    participants =
                                        participants,

                                    lastMessage =
                                        lastMessage,

                                    lastMessageTime =
                                        lastMessageTime,

                                    lastMessageSenderId =
                                        doc.getString(
                                            "lastMessageSenderId"
                                        ).orEmpty(),

                                    otherUserId =
                                        otherUid,

                                    unreadBy =
                                        unreadBy
                                )
                            }
                            ?: emptyList()

                    trySend(chats)
                }

        awaitClose {
            listener.remove()
        }
    }


    override suspend fun markChatAsRead(
        chatId: String,
        userId: String
    ): Result<Unit> = runCatching {

        require(chatId.isNotBlank()) {
            "Chat is unavailable"
        }

        require(userId.isNotBlank()) {
            "Session expired"
        }

        val chatRef =
            firestore.collection(CHATS)
                .document(chatId)

        firestore.runTransaction { transaction ->

            val snapshot =
                transaction.get(chatRef)

            if (!snapshot.exists()) {
                error("Chat not found")
            }

            val participants =
                (snapshot.get("participants") as? List<*>)
                    ?.mapNotNull {
                        it?.toString()
                    }
                    ?.distinct()
                    ?: emptyList()

            if (
                participants.size != 2 ||
                userId !in participants
            ) {
                error(
                    "You cannot access this chat"
                )
            }

            val unreadBy =
                (snapshot.get("unreadBy") as? List<*>)
                    ?.mapNotNull {
                        it?.toString()
                    }
                    ?: emptyList()

            val updatedUnreadBy =
                unreadBy.filterNot {
                    it == userId
                }

            if (updatedUnreadBy != unreadBy) {

                transaction.update(
                    chatRef,
                    "unreadBy",
                    updatedUnreadBy
                )
            }

            Unit
        }.await()
    }
}