package com.hiskytechs.muhallinewuserapp.supplier.Adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Models.ChatMessageType
import com.hiskytechs.muhallinewuserapp.network.ApiConfig
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierConversationBinding
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierMessageBinding
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierChatMessage
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierConversation
import com.hiskytechs.muhallinewuserapp.supplier.Utill.initials

class SupplierConversationAdapter(
    private var items: List<SupplierConversation>,
    private val onClick: (SupplierConversation) -> Unit
) : RecyclerView.Adapter<SupplierConversationAdapter.SupplierConversationViewHolder>() {

    inner class SupplierConversationViewHolder(
        private val binding: ItemSupplierConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierConversation) {
            val context = binding.root.context
            binding.tvAvatar.text = initials(item.retailerName)
            binding.tvAvatar.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, item.accentColorRes)
            )
            binding.tvRetailerName.text = item.retailerName
            binding.tvLastMessage.text = item.lastMessage
            binding.tvTime.text = item.timeLabel
            binding.tvUnread.visibility = if (item.unreadCount > 0) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvUnread.text = item.unreadCount.toString()
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierConversationViewHolder {
        val binding = ItemSupplierConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierConversationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<SupplierConversation>) {
        items = newItems
        notifyDataSetChanged()
    }
}

class SupplierMessageAdapter(
    private var items: List<SupplierChatMessage>
) : RecyclerView.Adapter<SupplierMessageAdapter.SupplierMessageViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var playingMessageId: String? = null

    inner class SupplierMessageViewHolder(
        private val binding: ItemSupplierMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierChatMessage) {
            val context = binding.root.context
            val isVoice = item.type == ChatMessageType.VOICE
            binding.tvMessage.text = item.message
            binding.tvMessage.visibility = if (isVoice || item.message.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
            binding.tvTime.text = item.timeLabel
            binding.layoutRoot.gravity = if (item.isMine) Gravity.END else Gravity.START
            binding.layoutBubble.setBackgroundResource(
                if (item.isMine) {
                    R.drawable.bg_supplier_message_me
                } else {
                    R.drawable.bg_supplier_message_other
                }
            )
            val messageColor = if (item.isMine) Color.WHITE else ContextCompat.getColor(context, R.color.text_dark)
            val timeColor = if (item.isMine) {
                ContextCompat.getColor(context, R.color.white)
            } else {
                ContextCompat.getColor(context, R.color.supplier_text_secondary)
            }
            val voicePrimaryColor = if (item.isMine) {
                ContextCompat.getColor(context, R.color.white)
            } else {
                ContextCompat.getColor(context, R.color.text_dark)
            }
            val voiceSecondaryColor = if (item.isMine) {
                ContextCompat.getColor(context, R.color.white)
            } else {
                ContextCompat.getColor(context, R.color.supplier_text_secondary)
            }
            binding.tvMessage.setTextColor(messageColor)
            binding.tvTime.setTextColor(timeColor)
            binding.tvVoiceStatus.setTextColor(voicePrimaryColor)
            binding.tvVoiceDuration.setTextColor(voiceSecondaryColor)

            binding.layoutVoiceCard.setBackgroundResource(
                if (item.isMine) R.drawable.bg_chat_voice_card_me else R.drawable.bg_chat_voice_card_other
            )
            binding.ivVoiceIcon.setBackgroundResource(
                if (item.isMine) R.drawable.bg_chat_media_thumb_me else R.drawable.bg_chat_media_thumb_other
            )
            binding.ivVoiceIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (item.isMine) R.color.white else R.color.primary
                )
            )
            binding.pbVoiceWave.progressTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (item.isMine) R.color.white else R.color.primary
                )
            )
            binding.pbVoiceWave.progressBackgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (item.isMine) R.color.white else R.color.status_transit_bg
                )
            )

            binding.layoutVoiceCard.visibility = if (isVoice) android.view.View.VISIBLE else android.view.View.GONE
            if (isVoice) {
                val source = ApiConfig.resolveMediaUrl(item.message)
                val isPlaying = playingMessageId == item.id && mediaPlayer?.isPlaying == true
                binding.tvVoiceStatus.text = if (isPlaying) {
                    context.getString(R.string.supplier_voice_playing)
                } else {
                    context.getString(R.string.supplier_voice_message)
                }
                binding.tvVoiceDuration.text = item.voiceDuration.ifBlank { context.getString(R.string.supplier_voice_tap_to_play) }
                binding.pbVoiceWave.isIndeterminate = isPlaying
                binding.ivVoiceIcon.setImageResource(if (isPlaying) R.drawable.ic_remove_24 else R.drawable.ic_play_arrow_24)
                binding.layoutVoiceCard.setOnClickListener { toggleVoice(item.id, source) }
                binding.ivVoiceIcon.setOnClickListener { toggleVoice(item.id, source) }
            } else {
                binding.layoutVoiceCard.setOnClickListener(null)
                binding.ivVoiceIcon.setOnClickListener(null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierMessageViewHolder {
        val binding = ItemSupplierMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierMessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierMessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<SupplierChatMessage>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun toggleVoice(messageId: String, source: String) {
        if (playingMessageId == messageId && mediaPlayer?.isPlaying == true) {
            stopVoice()
        } else {
            playVoice(messageId, source)
        }
    }

    private fun playVoice(messageId: String, source: String) {
        stopVoice()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(source)
            setOnCompletionListener {
                stopVoice()
            }
            setOnErrorListener { _, _, _ ->
                stopVoice()
                true
            }
            setOnPreparedListener { player ->
                playingMessageId = messageId
                player.start()
                notifyDataSetChanged()
            }
            prepareAsync()
        }
    }

    private fun stopVoice() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        playingMessageId = null
        notifyDataSetChanged()
    }
}
