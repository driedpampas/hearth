package org.eu.nl.syu.charchat.data

enum class MessageAuthor {
    USER, AI, SYSTEM
}

data class ChatMessage(
    val id: String,
    val text: String,
    val author: MessageAuthor,
    val isHiddenFromAi: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isUser: Boolean get() = author == MessageAuthor.USER
}
