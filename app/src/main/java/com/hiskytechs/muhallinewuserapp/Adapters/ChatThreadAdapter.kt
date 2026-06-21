package com.hiskytechs.muhallinewuserapp.Adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.Models.ChatThread
import com.hiskytechs.muhallinewuserapp.Ui.ChatConversationActivity
import com.hiskytechs.muhallinewuserapp.databinding.ItemChatThreadBinding

class ChatThreadAdapter(
    private var threads: List<ChatThread>
) : RecyclerView.Adapter<ChatThreadAdapter.ChatThreadViewHolder>() {

    inner class ChatThreadViewHolder(val binding: ItemChatThreadBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatThreadViewHolder {
        val binding = ItemChatThreadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatThreadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatThreadViewHolder, position: Int) {
        val thread = threads[position]
        holder.binding.apply {
            tvSupplierName.text = thread.supplierName
            tvLocation.text = thread.supplierLocation
            tvMessage.text = thread.lastMessage
            tvTime.text = thread.lastSeen
            if (thread.unreadCount > 0) {
                tvUnreadCount.text = thread.unreadCount.toString()
            } else {
                tvUnreadCount.text = ""
            }
            tvUnreadCount.alpha = if (thread.unreadCount > 0) 1f else 0f

            root.setOnClickListener {
                val intent = Intent(it.context, ChatConversationActivity::class.java).apply {
                    putExtra(ChatConversationActivity.EXTRA_THREAD_ID, thread.threadId)
                    putExtra(ChatConversationActivity.EXTRA_SUPPLIER_NAME, thread.supplierName)
                    putExtra(ChatConversationActivity.EXTRA_SUPPLIER_LOCATION, thread.supplierLocation)
                }
                it.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = threads.size

    fun updateItems(newThreads: List<ChatThread>) {
        threads = newThreads
        notifyDataSetChanged()
    }
}
