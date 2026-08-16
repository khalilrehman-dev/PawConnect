package com.example.authapp.ui.Chat

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.authapp.R
import com.example.authapp.model.Message
import com.example.authapp.presentation.chat.ChatEvent
import com.example.authapp.presentation.chat.ChatUiState
import com.example.authapp.presentation.chat.ChatViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class ChatActivity :
    AppCompatActivity() {

    private val viewModel:
            ChatViewModel by viewModels()


    private lateinit var toolbar:
            MaterialToolbar


    private lateinit var recyclerView:
            RecyclerView

    private lateinit var adapter:
            MessageAdapter


    private lateinit var layoutLoading:
            View

    private lateinit var layoutEmpty:
            View

    private lateinit var layoutError:
            View

    private lateinit var tvChatError:
            TextView


    private lateinit var layoutMessageComposer:
            View

    private lateinit var etMessage:
            TextInputEditText

    private lateinit var btnSend:
            FloatingActionButton

    private lateinit var btnRetry:
            View


    private var initialChatId =
        ""

    private var otherUserId =
        ""

    private var otherName =
        "Chat"


    private var isSending =
        false


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        enableEdgeToEdge()


        setContentView(
            R.layout.activity_chat
        )


        /*
         * Do not automatically open the keyboard
         * when the conversation screen starts.
         */
        window.setSoftInputMode(
            WindowManager.LayoutParams
                .SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    WindowManager.LayoutParams
                        .SOFT_INPUT_ADJUST_RESIZE
        )


        readIntentData()

        bindViews()

        applySystemInsets()

        setupToolbar()

        setupRecyclerView()

        setupClicks()


        /*
         * If neither a chat ID nor another user's
         * UID exists, there is nothing valid to open.
         */
        if (
            initialChatId.isBlank() &&
            otherUserId.isBlank()
        ) {

            showError(
                message =
                    "Conversation information is missing.",

                canRetry =
                    false
            )

            return
        }


        observeViewModel()


        /*
         * Prevent duplicate open/create operations across
         * configuration changes.
         *
         * The ViewModel survives rotation, so if it is
         * already Opening or Ready we simply observe it.
         */
        if (
            viewModel.uiState.value
                    is ChatUiState.Idle
        ) {

            openInitialConversation()
        }
    }


    private fun readIntentData() {

        initialChatId =
            intent
                .getStringExtra(
                    "chatId"
                )
                .orEmpty()


        otherUserId =
            intent
                .getStringExtra(
                    "otherUserId"
                )
                .orEmpty()


        otherName =
            intent
                .getStringExtra(
                    "otherName"
                )
                ?.trim()
                .orEmpty()
                .ifBlank {

                    "Chat"
                }
    }


    private fun bindViews() {

        toolbar =
            findViewById(
                R.id.toolbarChat
            )


        recyclerView =
            findViewById(
                R.id.recyclerView
            )


        layoutLoading =
            findViewById(
                R.id.layoutChatLoading
            )


        layoutEmpty =
            findViewById(
                R.id.layoutChatEmpty
            )


        layoutError =
            findViewById(
                R.id.layoutChatError
            )


        tvChatError =
            findViewById(
                R.id.tvChatError
            )


        layoutMessageComposer =
            findViewById(
                R.id.layoutMessageComposer
            )


        etMessage =
            findViewById(
                R.id.etMessage
            )


        btnSend =
            findViewById(
                R.id.btnSend
            )


        btnRetry =
            findViewById(
                R.id.btnRetryChat
            )
    }


    private fun applySystemInsets() {

        val root =
            findViewById<View>(
                R.id.rootChat
            )


        ViewCompat
            .setOnApplyWindowInsetsListener(
                root
            ) { view, insets ->

                val systemBars =
                    insets.getInsets(
                        WindowInsetsCompat
                            .Type
                            .systemBars()
                    )


                view.setPadding(
                    0,
                    systemBars.top,
                    0,
                    systemBars.bottom
                )


                insets
            }
    }


    private fun setupToolbar() {

        setSupportActionBar(
            toolbar
        )


        supportActionBar?.apply {

            title =
                otherName


            setDisplayHomeAsUpEnabled(
                true
            )
        }
    }


    private fun setupRecyclerView() {

        adapter =
            MessageAdapter(
                viewModel.myUid
            )


        recyclerView.layoutManager =
            LinearLayoutManager(
                this
            ).apply {

                stackFromEnd =
                    true
            }


        recyclerView.adapter =
            adapter
    }


    private fun setupClicks() {

        btnRetry.setOnClickListener {

            openInitialConversation()
        }


        btnSend.setOnClickListener {

            sendCurrentMessage()
        }
    }


    private fun openInitialConversation() {

        /*
         * Existing conversation from Inbox.
         */
        if (
            initialChatId.isNotBlank()
        ) {

            viewModel.initWithChatId(
                initialChatId
            )

            return
        }


        /*
         * New/existing deterministic conversation
         * started from Vet Detail or another profile.
         */
        if (
            otherUserId.isNotBlank()
        ) {

            viewModel.openChat(
                otherUserId
            )

            return
        }


        showError(
            message =
                "Conversation information is missing.",

            canRetry =
                false
        )
    }


    private fun sendCurrentMessage() {

        if (
            isSending
        ) {

            return
        }


        val text =
            etMessage
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        if (
            text.isBlank()
        ) {

            return
        }


        /*
         * Keep the typed text until MessageSent
         * is actually received.
         */
        setSending(
            true
        )


        viewModel.sendMessage(
            text
        )
    }


    private fun observeViewModel() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {


                // =================================================
                // Conversation screen state
                // =================================================

                launch {

                    viewModel
                        .uiState
                        .collect { state ->

                            when (state) {

                                ChatUiState.Idle -> {

                                    showOpening()
                                }


                                ChatUiState.Opening -> {

                                    showOpening()
                                }


                                ChatUiState.Ready -> {

                                    renderReadyContent(
                                        viewModel
                                            .messages
                                            .value
                                    )
                                }


                                is ChatUiState.Error -> {

                                    showError(
                                        message =
                                            state.message,

                                        canRetry =
                                            true
                                    )
                                }
                            }
                        }
                }


                // =================================================
                // Realtime messages
                // =================================================

                launch {

                    viewModel
                        .messages
                        .collect { messages ->

                            adapter.submitList(
                                messages
                            )


                            /*
                             * Do not show the empty-state until
                             * ChatUiState.Ready confirms the first
                             * Firestore snapshot was actually received.
                             */
                            if (
                                viewModel
                                    .uiState
                                    .value
                                        is ChatUiState.Ready
                            ) {

                                renderReadyContent(
                                    messages
                                )
                            }


                            if (
                                messages.isNotEmpty()
                            ) {

                                recyclerView.post {

                                    recyclerView
                                        .scrollToPosition(
                                            messages.lastIndex
                                        )
                                }
                            }
                        }
                }


                // =================================================
                // One-time events
                // =================================================

                launch {

                    viewModel
                        .events
                        .collect { event ->

                            when (event) {

                                is ChatEvent.ChatReady -> {

                                    /*
                                     * Clear unread state now that the
                                     * conversation is genuinely loaded.
                                     */
                                    viewModel.markChatAsRead(
                                        event.chatId
                                    )
                                }


                                ChatEvent.MessageSent -> {

                                    /*
                                     * Clear the field ONLY after
                                     * repository send succeeds.
                                     */
                                    etMessage
                                        .text
                                        ?.clear()


                                    setSending(
                                        false
                                    )


                                    etMessage
                                        .requestFocus()
                                }


                                is ChatEvent.Error -> {

                                    /*
                                     * Event errors here are mainly send
                                     * failures. Preserve typed text.
                                     */
                                    setSending(
                                        false
                                    )


                                    Toast.makeText(
                                        this@ChatActivity,
                                        event.message,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                }
            }
        }
    }


    // =========================================================
    // Screen rendering
    // =========================================================

    private fun showOpening() {

        isSending =
            false


        layoutLoading.visibility =
            View.VISIBLE


        recyclerView.visibility =
            View.GONE


        layoutEmpty.visibility =
            View.GONE


        layoutError.visibility =
            View.GONE


        layoutMessageComposer.visibility =
            View.GONE
    }


    private fun renderReadyContent(
        messages: List<Message>
    ) {

        layoutLoading.visibility =
            View.GONE


        layoutError.visibility =
            View.GONE


        layoutMessageComposer.visibility =
            View.VISIBLE


        etMessage.isEnabled =
            !isSending


        btnSend.isEnabled =
            !isSending


        if (
            messages.isEmpty()
        ) {

            /*
             * This is a genuine successfully loaded
             * conversation with zero messages.
             */
            recyclerView.visibility =
                View.GONE


            layoutEmpty.visibility =
                View.VISIBLE

        } else {

            layoutEmpty.visibility =
                View.GONE


            recyclerView.visibility =
                View.VISIBLE
        }
    }


    private fun showError(
        message: String,
        canRetry: Boolean
    ) {

        isSending =
            false


        layoutLoading.visibility =
            View.GONE


        recyclerView.visibility =
            View.GONE


        layoutEmpty.visibility =
            View.GONE


        layoutMessageComposer.visibility =
            View.GONE


        tvChatError.text =
            message


        btnRetry.visibility =
            if (
                canRetry
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        layoutError.visibility =
            View.VISIBLE
    }


    private fun setSending(
        sending: Boolean
    ) {

        isSending =
            sending


        etMessage.isEnabled =
            !sending


        btnSend.isEnabled =
            !sending


        btnSend.alpha =
            if (
                sending
            ) {

                0.55f

            } else {

                1f
            }
    }


    override fun onSupportNavigateUp():
            Boolean {

        onBackPressedDispatcher
            .onBackPressed()


        return true
    }
}


// =============================================================
// Message Adapter
// =============================================================

class MessageAdapter(
    private val myUid: String
) : RecyclerView.Adapter<
        RecyclerView.ViewHolder
        >() {

    companion object {

        private const val TYPE_SENT =
            1

        private const val TYPE_RECEIVED =
            2
    }


    private val items =
        mutableListOf<Message>()


    fun submitList(
        messages: List<Message>
    ) {

        items.clear()


        items.addAll(
            messages
        )


        notifyDataSetChanged()
    }


    override fun getItemViewType(
        position: Int
    ): Int {

        return if (
            items[position]
                .senderId ==
            myUid
        ) {

            TYPE_SENT

        } else {

            TYPE_RECEIVED
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater =
            LayoutInflater
                .from(
                    parent.context
                )


        return if (
            viewType ==
            TYPE_SENT
        ) {

            SentViewHolder(
                inflater.inflate(
                    R.layout.item_message_sent,
                    parent,
                    false
                )
            )

        } else {

            ReceivedViewHolder(
                inflater.inflate(
                    R.layout.item_message_received,
                    parent,
                    false
                )
            )
        }
    }


    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        val message =
            items[position]


        when (holder) {

            is SentViewHolder -> {

                holder.bind(
                    message
                )
            }


            is ReceivedViewHolder -> {

                holder.bind(
                    message
                )
            }
        }
    }


    override fun getItemCount():
            Int {

        return items.size
    }


    inner class SentViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(
        itemView
    ) {

        private val tvMessage:
                TextView =
            itemView.findViewById(
                R.id.tvMessage
            )


        private val tvTime:
                TextView =
            itemView.findViewById(
                R.id.tvTime
            )


        fun bind(
            message: Message
        ) {

            tvMessage.text =
                message.text


            tvTime.text =
                formatTime(
                    message.timestamp
                )
        }
    }


    inner class ReceivedViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(
        itemView
    ) {

        private val tvMessage:
                TextView =
            itemView.findViewById(
                R.id.tvMessage
            )


        private val tvTime:
                TextView =
            itemView.findViewById(
                R.id.tvTime
            )


        fun bind(
            message: Message
        ) {

            tvMessage.text =
                message.text


            tvTime.text =
                formatTime(
                    message.timestamp
                )
        }
    }


    private fun formatTime(
        timestamp: Long
    ): String {

        if (
            timestamp <= 0L
        ) {

            return ""
        }


        return DateFormat
            .format(
                "hh:mm a",
                timestamp
            )
            .toString()
    }
}