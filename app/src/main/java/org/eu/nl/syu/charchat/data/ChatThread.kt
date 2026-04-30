package org.eu.nl.syu.charchat.data

import java.util.UUID

data class ChatThread(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = createdAt,
    val sequenceId: Int = 0
)