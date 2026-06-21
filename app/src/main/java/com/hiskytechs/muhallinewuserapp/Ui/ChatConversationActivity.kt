package com.hiskytechs.muhallinewuserapp.Ui

import android.Manifest
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.Adapters.ChatMessageAdapter
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.Models.ChatMessage
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.ActivityChatConversationBinding
import com.hiskytechs.muhallinewuserapp.Utill.VoiceNoteClip
import com.hiskytechs.muhallinewuserapp.Utill.VoiceNoteRecorder

class ChatConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatConversationBinding
    private lateinit var messageAdapter: ChatMessageAdapter
    private lateinit var voiceRecorder: VoiceNoteRecorder
    private val messages = mutableListOf<ChatMessage>()
    private var supplierName: String = ""
    private var supplierId: Int = 0
    private var threadId: Int = 0
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
            Toast.makeText(this, getString(R.string.voice_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        voiceRecorder = VoiceNoteRecorder(this)

        threadId = intent.getIntExtra(EXTRA_THREAD_ID, 0)
        supplierId = intent.getIntExtra(EXTRA_SUPPLIER_ID, 0)
        supplierName = intent.getStringExtra(EXTRA_SUPPLIER_NAME).orEmpty()
        val supplierLocation = intent.getStringExtra(EXTRA_SUPPLIER_LOCATION).orEmpty()

        binding.toolbar.title = supplierName
        binding.tvSupplierLocation.text = supplierLocation
        binding.tvSupplierStatus.text = getString(R.string.online_for_media_and_voice_updates)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupMessagesRecycler()
        setupComposer()
        resolveConversation()
    }

    private fun setupMessagesRecycler() {
        messageAdapter = ChatMessageAdapter(
            supplierName = supplierName.ifBlank { getString(R.string.supplier_fallback) },
            messages = messages
        )
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = messageAdapter
    }

    private fun resolveConversation() {
        if (threadId > 0) {
            loadConversation()
            return
        }

        val cachedThread = AppData.findThreadBySupplierName(supplierName)
        if (cachedThread != null) {
            threadId = cachedThread.threadId
            loadConversation()
            return
        }

        AppData.loadChats(
            onSuccess = { threads ->
                val match = threads.firstOrNull {
                    it.supplierName.equals(supplierName, ignoreCase = true)
                }
                if (match == null) {
                    startNewConversation()
                } else {
                    threadId = match.threadId
                    loadConversation()
                }
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun startNewConversation() {
        if (supplierId <= 0) {
            Toast.makeText(this, getString(R.string.no_messages_yet), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        AppData.startChatWithSupplier(
            supplierId = supplierId,
            onSuccess = { thread ->
                threadId = thread.threadId
                if (supplierName.isBlank()) {
                    supplierName = thread.supplierName
                    binding.toolbar.title = supplierName
                }
                loadConversation()
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun loadConversation() {
        AppData.loadConversation(
            threadId = threadId,
            onSuccess = { conversation ->
                messages.clear()
                messages.addAll(conversation)
                messageAdapter.notifyDataSetChanged()
                binding.rvMessages.post {
                    binding.rvMessages.scrollToPosition((messageAdapter.itemCount - 1).coerceAtLeast(0))
                }
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun scrollToBottom() {
        binding.rvMessages.post {
            binding.rvMessages.scrollToPosition((messageAdapter.itemCount - 1).coerceAtLeast(0))
        }
    }

    private fun setupComposer() {
        binding.etMessage.doAfterTextChanged { text ->
            binding.layoutSendAction.alpha = if (text.isNullOrBlank()) 0.65f else 1f
        }

        binding.layoutSendAction.setOnClickListener {
            val text = binding.etMessage.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) return@setOnClickListener
            if (threadId <= 0) return@setOnClickListener

            AppData.sendMessage(
                threadId = threadId,
                message = text,
                onSuccess = { updatedMessages ->
                    messages.clear()
                    messages.addAll(updatedMessages)
                    messageAdapter.notifyDataSetChanged()
                    binding.etMessage.setText("")
                    scrollToBottom()
                },
                onError = { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            )
        }

        binding.layoutMediaAction.setOnClickListener {
            Toast.makeText(this, getString(R.string.media_coming_soon), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, it.message ?: getString(R.string.voice_notes_coming_soon), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, getString(R.string.voice_notes_coming_soon), Toast.LENGTH_SHORT).show()
            return
        }
        AppData.sendMessage(
            threadId = threadId,
            message = "",
            messageType = "voice",
            voiceDataUrl = voiceDataUrl,
            voiceDuration = clip.durationLabel,
            onSuccess = { updatedMessages ->
                messages.clear()
                messages.addAll(updatedMessages)
                messageAdapter.notifyDataSetChanged()
                pendingVoiceClip = null
                runCatching { clip.file.delete() }
                renderVoiceRecorder()
                scrollToBottom()
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
            isRecordingVoice -> getString(R.string.voice_recording)
            hasDraft -> getString(R.string.voice_ready)
            else -> getString(R.string.voice_ready)
        }
        binding.tvVoiceRecorderTimer.text = when {
            isRecordingVoice -> formatDuration(System.currentTimeMillis() - recordingStartedAtMs)
            hasDraft -> pendingVoiceClip?.durationLabel.orEmpty()
            else -> ""
        }
        binding.tvVoiceRecorderSend.visibility = if (hasDraft) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvVoiceRecorderCancel.text = if (isRecordingVoice) getString(R.string.voice_cancel) else getString(R.string.voice_cancel)
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
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_SUPPLIER_ID = "supplier_id"
        const val EXTRA_SUPPLIER_NAME = "supplier_name"
        const val EXTRA_SUPPLIER_LOCATION = "supplier_location"
    }

    override fun onDestroy() {
        voiceTimerHandler.removeCallbacksAndMessages(null)
        runCatching { previewPlayer?.release() }
        runCatching { voiceRecorder.cancel() }
        super.onDestroy()
    }
}
