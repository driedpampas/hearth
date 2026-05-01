package org.eu.nl.syu.charchat.data

import java.util.UUID

/**
 * Message model supporting system-level notes and AI hidden states.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isHiddenFromAi: Boolean = false,
    val modelReference: String? = null,
    val generationTimeMs: Long? = null,
    val tokensPerSecond: Float? = null,
    val parentId: String? = null,
    val versionGroupId: String? = null,
    val versionIndex: Int = 0
)

enum class MessageRole {
    USER, MODEL, SYSTEM
}
