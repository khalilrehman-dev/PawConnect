package com.example.authapp.ui.Chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.authapp.R
import com.example.authapp.model.Message
import com.example.authapp.presentation.chat.ChatEvent
import com.example.authapp.presentation.chat.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val chatId      = intent.getStringExtra("chatId")      ?: ""
        val otherUserId = intent.getStringExtra("otherUserId") ?: ""
        val otherName   = intent.getStringExtra("otherName")   ?: "Chat"

        recyclerView = findViewById(R.id.recyclerView)
        etMessage    = findViewById(R.id.etMessage)
        btnSend      = findViewById(R.id.btnSend)

        adapter = MessageAdapter(viewModel.myUid)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = adapter

        supportActionBar?.apply {
            title = otherName
            setDisplayHomeAsUpEnabled(true)
        }

        // If chatId passed directly (from inbox) use it
        // If otherUserId passed (from profile) create/get chat first
        if (chatId.isNotEmpty()) {
            viewModel.initWithChatId(chatId)
        } else if (otherUserId.isNotEmpty()) {
            viewModel.openChat(otherUserId)
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                etMessage.text.clear()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.messages.collect { messages ->
                        adapter.submitList(messages)
                        if (messages.isNotEmpty()) {
                            recyclerView.scrollToPosition(messages.size - 1)
                        }
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is ChatEvent.Error -> Toast.makeText(this@ChatActivity, event.message, Toast.LENGTH_SHORT).show()
                            else -> { }
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

// ── MessageAdapter ────────────────────────────────────────────────────────────

class MessageAdapter(
    private val myUid: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SENT     = 1
        private const val TYPE_RECEIVED = 2
    }

    private val items = mutableListOf<Message>()

    fun submitList(list: List<Message>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (items[position].senderId == myUid) TYPE_SENT else TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SENT) {
            SentViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false))
        } else {
            ReceivedViewHolder(inflater.inflate(R.layout.item_message_received, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = items[position]
        if (holder is SentViewHolder) holder.bind(msg)
        else if (holder is ReceivedViewHolder) holder.bind(msg)
    }

    override fun getItemCount() = items.size

    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView    = itemView.findViewById(R.id.tvTime)
        fun bind(msg: Message) {
            tvMessage.text = msg.text
            tvTime.text    = android.text.format.DateFormat.format("hh:mm a", msg.timestamp).toString()
        }
    }

    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView    = itemView.findViewById(R.id.tvTime)
        fun bind(msg: Message) {
            tvMessage.text = msg.text
            tvTime.text    = android.text.format.DateFormat.format("hh:mm a", msg.timestamp).toString()
        }
    }
}