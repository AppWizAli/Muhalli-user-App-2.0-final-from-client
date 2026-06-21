package com.hiskytechs.muhallinewuserapp.Utill

import android.content.Context
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Base64
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

data class VoiceNoteClip(
    val file: File,
    val durationLabel: String
)

class VoiceNoteRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var startedAtMs: Long = 0L

    val isRecording: Boolean
        get() = recorder != null

    fun start(): File {
        if (isRecording) return requireNotNull(recordingFile)
        val file = File.createTempFile("voice_note_", ".m4a", context.cacheDir)
        recordingFile = file
        startedAtMs = SystemClock.elapsedRealtime()

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        return file
    }

    fun stop(): VoiceNoteClip? {
        val activeRecorder = recorder ?: return null
        val file = recordingFile ?: return null
        val durationMs = (SystemClock.elapsedRealtime() - startedAtMs).coerceAtLeast(0L)
        runCatching { activeRecorder.stop() }
        release()
        return if (file.exists() && file.length() > 0) {
            VoiceNoteClip(file, formatDuration(durationMs))
        } else {
            file.delete()
            null
        }
    }

    fun cancel() {
        release()
        recordingFile?.delete()
        recordingFile = null
    }

    fun toDataUrl(file: File, mimeType: String = "audio/mp4"): String {
        val bytes = file.readBytes()
        return "data:$mimeType;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun release() {
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        startedAtMs = 0L
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs).coerceAtLeast(0L)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
