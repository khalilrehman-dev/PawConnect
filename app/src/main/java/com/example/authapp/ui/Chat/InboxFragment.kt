package com.example.authapp.ui.Chat

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.authapp.R
import com.example.authapp.presentation.chat.InboxUiItem
import com.example.authapp.presentation.chat.InboxUiState
import com.example.authapp.presentation.chat.InboxViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar


@AndroidEntryPoint
class InboxFragment :
    Fragment(R.layout.fragment_inbox) {

    private val viewModel:
            InboxViewModel by viewModels()


    private lateinit var recyclerView:
            RecyclerView

    private lateinit var adapter:
            InboxFragmentAdapter


    private lateinit var layoutLoading:
            View

    private lateinit var layoutEmpty:
            View

    private lateinit var layoutError:
            View


    private lateinit var tvError:
            TextView

    private lateinit var btnRetry:
            MaterialButton


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )


        bindViews(
            view
        )

        setupRecyclerView()

        setupClicks()

        observeInbox()
    }


    override fun onResume() {
        super.onResume()

        /*
         * Restart the realtime inbox listener
         * when returning from ChatActivity.
         *
         * This also refreshes unread indicators.
         */
        viewModel.loadInbox()
    }


    private fun bindViews(
        view: View
    ) {

        recyclerView =
            view.findViewById(
                R.id.recyclerInbox
            )


        layoutLoading =
            view.findViewById(
                R.id.layoutInboxLoading
            )


        layoutEmpty =
            view.findViewById(
                R.id.layoutInboxEmpty
            )


        layoutError =
            view.findViewById(
                R.id.layoutInboxError
            )


        tvError =
            view.findViewById(
                R.id.tvInboxError
            )


        btnRetry =
            view.findViewById(
                R.id.btnRetryInbox
            )
    }


    private fun setupRecyclerView() {

        adapter =
            InboxFragmentAdapter(
                onConversationClick = { conversation ->

                    openConversation(
                        conversation
                    )
                }
            )


        recyclerView.layoutManager =
            LinearLayoutManager(
                requireContext()
            )


        recyclerView.adapter =
            adapter
    }


    private fun setupClicks() {

        btnRetry.setOnClickListener {

            viewModel.loadInbox()
        }
    }


    private fun observeInbox() {

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                viewLifecycleOwner
                    .repeatOnLifecycle(
                        Lifecycle.State.STARTED
                    ) {

                        viewModel
                            .uiState
                            .collect { state ->

                                when (state) {

                                    InboxUiState.Loading -> {

                                        showLoading()
                                    }


                                    is InboxUiState.Success -> {

                                        showConversations(
                                            state.conversations
                                        )
                                    }


                                    InboxUiState.Empty -> {

                                        showEmpty()
                                    }


                                    is InboxUiState.Error -> {

                                        showError(
                                            state.message
                                        )
                                    }
                                }
                            }
                    }
            }
    }


    private fun openConversation(
        conversation: InboxUiItem
    ) {

        startActivity(
            Intent(
                requireContext(),
                ChatActivity::class.java
            ).apply {

                putExtra(
                    "chatId",
                    conversation.chatId
                )


                putExtra(
                    "otherUserId",
                    conversation.otherUserId
                )


                putExtra(
                    "otherName",
                    conversation.otherUserName
                )
            }
        )
    }


    private fun showLoading() {

        layoutLoading.visibility =
            View.VISIBLE


        recyclerView.visibility =
            View.GONE


        layoutEmpty.visibility =
            View.GONE


        layoutError.visibility =
            View.GONE
    }


    private fun showConversations(
        conversations:
        List<InboxUiItem>
    ) {

        layoutLoading.visibility =
            View.GONE


        layoutEmpty.visibility =
            View.GONE


        layoutError.visibility =
            View.GONE


        adapter.submitList(
            conversations
        )


        recyclerView.visibility =
            View.VISIBLE
    }


    private fun showEmpty() {

        layoutLoading.visibility =
            View.GONE


        recyclerView.visibility =
            View.GONE


        layoutError.visibility =
            View.GONE


        layoutEmpty.visibility =
            View.VISIBLE
    }


    private fun showError(
        message: String
    ) {

        layoutLoading.visibility =
            View.GONE


        recyclerView.visibility =
            View.GONE


        layoutEmpty.visibility =
            View.GONE


        tvError.text =
            message


        layoutError.visibility =
            View.VISIBLE
    }
}


/*
 * RecyclerView adapter for the shared Owner/Vet Inbox.
 */
class InboxFragmentAdapter(
    private val onConversationClick:
        (InboxUiItem) -> Unit
) : RecyclerView.Adapter<
        InboxFragmentAdapter.ViewHolder
        >() {

    private val items =
        mutableListOf<InboxUiItem>()


    fun submitList(
        conversations:
        List<InboxUiItem>
    ) {

        items.clear()


        items.addAll(
            conversations
        )


        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_inbox,
                    parent,
                    false
                )


        return ViewHolder(
            view
        )
    }


    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        holder.bind(
            items[position]
        )
    }


    override fun getItemCount():
            Int {

        return items.size
    }


    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(
        itemView
    ) {

        private val ivProfile:
                ImageView =
            itemView.findViewById(
                R.id.ivInboxProfile
            )


        private val tvName:
                TextView =
            itemView.findViewById(
                R.id.tvName
            )


        private val tvLastMessage:
                TextView =
            itemView.findViewById(
                R.id.tvLastMessage
            )


        private val tvTime:
                TextView =
            itemView.findViewById(
                R.id.tvTime
            )


        private val unreadDot:
                View =
            itemView.findViewById(
                R.id.viewUnreadDot
            )


        fun bind(
            conversation:
            InboxUiItem
        ) {

            tvName.text =
                conversation
                    .otherUserName
                    .ifBlank {

                        "User"
                    }


            tvLastMessage.text =
                conversation
                    .lastMessage
                    .ifBlank {

                        "No messages yet"
                    }


            tvTime.text =
                formatTimestamp(
                    conversation
                        .lastMessageTime
                )


            bindUnreadState(
                conversation
                    .isUnread
            )


            bindProfileImage(
                conversation
                    .profileImageUrl
            )


            itemView.setOnClickListener {

                onConversationClick(
                    conversation
                )
            }
        }


        private fun bindUnreadState(
            isUnread: Boolean
        ) {

            unreadDot.visibility =
                if (isUnread) {

                    View.VISIBLE

                } else {

                    View.GONE
                }


            tvName.setTypeface(
                null,

                if (isUnread) {

                    Typeface.BOLD

                } else {

                    Typeface.NORMAL
                }
            )


            tvLastMessage.setTypeface(
                null,

                if (isUnread) {

                    Typeface.BOLD

                } else {

                    Typeface.NORMAL
                }
            )
        }


        private fun bindProfileImage(
            imageUrl: String
        ) {

            if (
                imageUrl.isBlank()
            ) {

                ivProfile.setImageResource(
                    R.drawable.ic_profile
                )


                return
            }


            ivProfile.load(
                imageUrl
            ) {

                crossfade(
                    true
                )


                placeholder(
                    R.drawable.ic_profile
                )


                error(
                    R.drawable.ic_profile
                )
            }
        }


        private fun formatTimestamp(
            timestamp: Long
        ): String {

            if (
                timestamp <= 0L
            ) {

                return ""
            }


            val messageDate =
                Calendar
                    .getInstance()
                    .apply {

                        timeInMillis =
                            timestamp
                    }


            val today =
                Calendar
                    .getInstance()


            val sameDay =
                messageDate.get(
                    Calendar.YEAR
                ) ==
                        today.get(
                            Calendar.YEAR
                        ) &&

                        messageDate.get(
                            Calendar.DAY_OF_YEAR
                        ) ==
                        today.get(
                            Calendar.DAY_OF_YEAR
                        )


            return if (sameDay) {

                DateFormat.format(
                    "hh:mm a",
                    timestamp
                ).toString()

            } else {

                DateFormat.format(
                    "dd MMM",
                    timestamp
                ).toString()
            }
        }
    }
}