package org.eu.nl.syu.charchat.data

data class Character(
    val id: String,
    val name: String,
    val avatarRes: Int? = null,
    val avatarUrl: String? = null,
    val tagline: String,
    val shortDescription: String,
    val systemPromptLore: String,
    val reminderMessage: String = "",
    val initialMessages: List<String> = emptyList(),
    val sceneBackgroundUrl: String? = null,
    val ambientMusicUrl: String? = null,
    val modelPath: String? = null,
    val isPredefined: Boolean
)
