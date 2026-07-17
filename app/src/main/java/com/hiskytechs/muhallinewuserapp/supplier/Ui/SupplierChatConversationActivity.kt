package com.hiskytechs.muhallinewuserapp.supplier.Ui

import android.Manifest
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Utill.KeyboardInsets
import com.hiskytechs.muhallinewuserapp.Utill.VoiceNoteClip
import com.hiskytechs.muhallinewuserapp.Utill.VoiceNoteRecorder
import com.hiskytechs.muhallinewuserapp.databinding.ActivitySupplierChatConversationBinding
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierMessageAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData
import com.hiskytechs.muhallinewuserapp.supplier.Utill.initials

class SupplierChatConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupplierChatConversationBinding
    private lateinit var messageAdapter: SupplierMessageAdapter
    private lateinit var voiceRecorder: VoiceNoteRecorder
    private var conversationId: String = ""
    private var pendingVoiceClip: VoiceNoteClip? = null
    private var previewPlayer: MediaPlayer? = null
    private var isRecordingVoice = false
    private var recordingStartedAtMs: Long = 0L
    private val voiceTimerHandler = Handler(Looper.getMainLooper())
    private val voiceTimerRunnable = object : Runnable {
        override fun run() {
            if (!isRecordingVoice) return
            binding.tvVoiceRecorderTimer.text = formatDuration(System.currentTimeMillis() - recordingStartedAtMs)
            voiceTimerHandler.postDelayed(this, 1000L)
        }
    }

    private val voicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceRecording()
        } else {
            Toast.makeText(this, getString(R.string.supplier_voice_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierChatConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KeyboardInsets.applyBottomPadding(binding.root)
        voiceRecorder = VoiceNoteRecorder(this)

        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID).orEmpty()
        binding.ivBack.setOnClickListener { finish() }

        messageAdapter = SupplierMessageAdapter(emptyList())
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = messageAdapter

        binding.ivSend.setOnClickListener {
            val message = binding.etMessage.text?.toString().orEmpty()
            if (message.isBlank()) return@setOnClickListener
            SupplierData.sendMessage(
                conversationId = conversationId,
                message = message,
                onSuccess = {
                    binding.etMessage.text?.clear()
                    loadMessages()
                },
                onError = { error ->
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            )
        }

        binding.layoutVoiceAction.setOnClickListener {
            if (isRecordingVoice) {
                stopVoiceRecording(saveDraft = true)
            } else {
                if (pendingVoiceClip != null) {
                    discardVoiceDraft()
                }
                voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        binding.tvVoiceRecorderCancel.setOnClickListener { discardVoiceDraft() }
        binding.tvVoiceRecorderSend.setOnClickListener { sendVoiceDraft() }
        binding.ivVoicePreviewPlay.setOnClickListener { playVoicePreview() }
        renderVoiceRecorder()

        bindConversation()
    }

    private fun bindConversation() {
        val conversation = SupplierData.findConversation(conversationId)
        if (conversation != null) {
            binding.tvAvatar.text = initials(conversation.retailerName)
            binding.tvRetailerName.text = conversation.retailerName
            loadMessages()
            return
        }

        SupplierData.refreshMessages(
            onSuccess = {
                val refreshedConversation = SupplierData.findConversation(conversationId)
                if (refreshedConversation == null) {
                    finish()
                } else {
                    binding.tvAvatar.text = initials(refreshedConversation.retailerName)
                    binding.tvRetailerName.text = refreshedConversation.retailerName
                    loadMessages()
                }
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun loadMessages() {
        SupplierData.loadConversation(
            conversationId = conversationId,
            onSuccess = { messages ->
                messageAdapter.updateItems(messages)
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.lastIndex)
                }
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun startVoiceRecording() {
        runCatching { previewPlayer?.stop() }
        runCatching { previewPlayer?.release() }
        previewPlayer = null
        pendingVoiceClip = null
        runCatching { voiceRecorder.start() }
            .onSuccess {
                isRecordingVoice = true
                recordingStartedAtMs = System.currentTimeMillis()
                renderVoiceRecorder()
                voiceTimerHandler.removeCallbacks(voiceTimerRunnable)
                voiceTimerHandler.post(voiceTimerRunnable)
            }
            .onFailure {
                isRecordingVoice = false
                Toast.makeText(this, it.message ?: getString(R.string.supplier_voice_permission_required), Toast.LENGTH_SHORT).show()
                renderVoiceRecorder()
            }
    }

    private fun stopVoiceRecording(saveDraft: Boolean) {
        if (!isRecordingVoice) return
        isRecordingVoice = false
        voiceTimerHandler.removeCallbacks(voiceTimerRunnable)
        val clip = runCatching { voiceRecorder.stop() }.getOrNull()
        pendingVoiceClip = if (saveDraft) clip else null
        if (!saveDraft) {
            voiceRecorder.cancel()
        }
        renderVoiceRecorder()
    }

    private fun discardVoiceDraft() {
        voiceTimerHandler.removeCallbacks(voiceTimerRunnable)
        if (isRecordingVoice) {
            isRecordingVoice = false
            runCatching { voiceRecorder.cancel() }
        } else {
            runCatching { pendingVoiceClip?.file?.delete() }
        }
        pendingVoiceClip = null
        runCatching { previewPlayer?.stop() }
        runCatching { previewPlayer?.release() }
        previewPlayer = null
        renderVoiceRecorder()
    }

    private fun sendVoiceDraft() {
        val clip = pendingVoiceClip ?: return
        val voiceDataUrl = runCatching { voiceRecorder.toDataUrl(clip.file) }.getOrNull()
        if (voiceDataUrl.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.supplier_voice_permission_required), Toast.LENGTH_SHORT).show()
            return
        }

        SupplierData.sendMessage(
            conversationId = conversationId,
            message = "",
            messageType = "voice",
            voiceDataUrl = voiceDataUrl,
            voiceDuration = clip.durationLabel,
            onSuccess = {
                binding.etMessage.text?.clear()
                pendingVoiceClip = null
                runCatching { clip.file.delete() }
                renderVoiceRecorder()
                loadMessages()
            },
            onError = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun playVoicePreview() {
        val clip = pendingVoiceClip ?: return
        runCatching { previewPlayer?.stop() }
        runCatching { previewPlayer?.release() }
        previewPlayer = MediaPlayer().apply {
            setDataSource(clip.file.absolutePath)
            setOnCompletionListener {
                runCatching { stop() }
                runCatching { release() }
                previewPlayer = null
            }
            prepare()
            start()
        }
    }

    private fun renderVoiceRecorder() {
        val hasDraft = pendingVoiceClip != null
        binding.layoutVoiceRecorder.visibility = if (isRecordingVoice || hasDraft) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvVoiceRecorderState.text = when {
            isRecordingVoice -> getString(R.string.supplier_voice_recording)
            hasDraft -> getString(R.string.supplier_voice_ready)
            else -> getString(R.string.supplier_voice_ready)
        }
        binding.tvVoiceRecorderTimer.text = when {
            isRecordingVoice -> formatDuration(System.currentTimeMillis() - recordingStartedAtMs)
            hasDraft -> pendingVoiceClip?.durationLabel.orEmpty()
            else -> ""
        }
        binding.tvVoiceRecorderSend.visibility = if (hasDraft) android.view.View.VISIBLE else android.view.View.GONE
        binding.ivVoicePreviewPlay.visibility = if (hasDraft) android.view.View.VISIBLE else android.view.View.GONE
        binding.layoutVoiceAction.setImageResource(
            if (isRecordingVoice) R.drawable.ic_remove_24 else R.drawable.ic_mic_24
        )
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format("%d:%02d", minutes, seconds)
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
    }

    override fun onDestroy() {
        voiceTimerHandler.removeCallbacksAndMessages(null)
        runCatching { previewPlayer?.release() }
        runCatching { voiceRecorder.cancel() }
        super.onDestroy()
    }
}
