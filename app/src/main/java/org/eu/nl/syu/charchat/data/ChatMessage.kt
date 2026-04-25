package org.eu.nl.syu.charchat.data

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
