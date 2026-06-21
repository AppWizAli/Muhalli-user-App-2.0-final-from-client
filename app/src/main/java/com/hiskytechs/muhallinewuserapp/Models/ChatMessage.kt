package com.hiskytechs.muhallinewuserapp.Models

enum class ChatParticipant {
    BUYER,
    SUPPLIER
}

enum class ChatMessageType {
    TEXT,
    MEDIA,
    VOICE
}

data class ChatMessage(
    val id: String,
    val participant: ChatParticipant,
    val type: ChatMessageType,
    val body: String = "",
    val timeLabel: String,
    val mediaLabel: String = "",
    val mediaTitle: String = "",
    val mediaSubtitle: String = "",
    val voiceDuration: String = "",
    val voiceStatus: String = "",
    val voiceProgress: Int = 0
)
