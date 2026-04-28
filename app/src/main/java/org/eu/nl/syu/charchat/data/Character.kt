package org.eu.nl.syu.charchat.data

import java.util.UUID

/**
 * Expanded Character model for immersive roleplay.
 */
data class Character(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tagline: String,
    val avatarUrl: String? = null,
    val systemPromptLore: String,
    val reminderMessage: String = "",
    val initialMessages: List<ChatMessage> = emptyList(),
    val modelReference: String, // Path or ID of the .litertlm file
    val temp: Float = 0.8f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val enableThinking: Boolean = false,
    val sceneBackgroundUrl: String? = null,
    val isPredefined: Boolean = false,
    val lastUsedAt: Long = 0L
)
