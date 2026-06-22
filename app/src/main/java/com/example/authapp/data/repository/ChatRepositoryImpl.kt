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

    override suspend fun getOrCreateChat(myUid: String, otherUid: String): Result<String> = runCatching {
        val chatId  = getChatId(myUid, otherUid)
        val docRef  = firestore.collection(CHATS).document(chatId)
        val snap    = docRef.get().await()
        if (!snap.exists()) {
            docRef.set(mapOf(
                "chatId"              to chatId,
                "participants"        to listOf(myUid, otherUid),
                "lastMessage"         to "",
                "lastMessageTime"     to 0L,
                "lastMessageSenderId" to ""
            )).await()
        }
        chatId
    }

    override fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore
            .collection(MESSAGES)
            .document(chatId)
            .collection(MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val msgs = snap?.documents?.mapNotNull { doc ->
                    Message(
                        id         = doc.getString("id")       ?: "",
                        chatId     = doc.getString("chatId")   ?: "",
                        senderId   = doc.getString("senderId") ?: "",
                        text       = doc.getString("text")     ?: "",
                        timestamp  = doc.getLong("timestamp")  ?: 0L
                    )
                } ?: emptyList()
                trySend(msgs)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(chatId: String, senderId: String, text: String): Result<Unit> = runCatching {
        val msgRef = firestore.collection(MESSAGES).document(chatId)
            .collection(MESSAGES).document()

        val msg = mapOf(
            "id"        to msgRef.id,
            "chatId"    to chatId,
            "senderId"  to senderId,
            "text"      to text.trim(),
            "timestamp" to System.currentTimeMillis()
        )
        msgRef.set(msg).await()

        // Update chat preview
        firestore.collection(CHATS).document(chatId).update(
            "lastMessage",         text.trim(),
            "lastMessageTime",     System.currentTimeMillis(),
            "lastMessageSenderId", senderId
        ).await()
    }

    override fun getInbox(uid: String): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection(CHATS)
            .whereArrayContains("participants", uid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val chats = snap?.documents?.mapNotNull { doc ->
                    val participants = doc.get("participants") as? List<*>
                    val otherUid = participants?.firstOrNull { it != uid }?.toString() ?: ""
                    Chat(
                        chatId              = doc.getString("chatId")              ?: "",
                        participants        = participants?.map { it.toString() }  ?: emptyList(),
                        lastMessage         = doc.getString("lastMessage")         ?: "",
                        lastMessageTime     = doc.getLong("lastMessageTime")       ?: 0L,
                        lastMessageSenderId = doc.getString("lastMessageSenderId") ?: "",
                        otherUserId         = otherUid
                    )
                } ?: emptyList()
                trySend(chats)
            }
        awaitClose { listener.remove() }
    }
}