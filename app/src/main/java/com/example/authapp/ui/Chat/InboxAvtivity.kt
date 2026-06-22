package com.example.authapp.ui.Chat

import android.content.Intent
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
import com.example.authapp.model.Chat
import com.example.authapp.presentation.chat.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InboxActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: InboxAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)

        recyclerView = findViewById(R.id.recyclerView)
        tvEmpty      = findViewById(R.id.tvEmpty)

        adapter = InboxAdapter { chat ->
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra("chatId",      chat.chatId)
                putExtra("otherUserId", chat.otherUserId)
                putExtra("otherName",   chat.otherUserName.ifEmpty { "User" })
            })
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        supportActionBar?.apply {
            title = "Messages"
            setDisplayHomeAsUpEnabled(true)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.inbox.collect { chats ->
                    tvEmpty.visibility      = if (chats.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (chats.isEmpty()) View.GONE else View.VISIBLE
                    adapter.submitList(chats)
                }
            }
        }

        viewModel.loadInbox()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

// ── InboxAdapter ──────────────────────────────────────────────────────────────

class InboxAdapter(
    private val onClick: (Chat) -> Unit
) : RecyclerView.Adapter<InboxAdapter.ViewHolder>() {

    private val items = mutableListOf<Chat>()

    fun submitList(list: List<Chat>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_inbox, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView        = itemView.findViewById(R.id.tvName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvTime: TextView        = itemView.findViewById(R.id.tvTime)

        fun bind(chat: Chat) {
            tvName.text        = chat.otherUserName.ifEmpty { "User" }
            tvLastMessage.text = chat.lastMessage.ifEmpty { "No messages yet" }
            tvTime.text        = if (chat.lastMessageTime > 0)
                android.text.format.DateFormat.format("hh:mm a", chat.lastMessageTime).toString()
            else ""
            itemView.setOnClickListener { onClick(chat) }
        }
    }
}