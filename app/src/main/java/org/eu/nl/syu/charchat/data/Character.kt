package org.eu.nl.syu.charchat.data

data class Character(
    val id: String,
    val name: String,
    val avatarRes: Int? = null,
    val avatarUrl: String? = null,
    val shortDescription: String,
    val systemPromptLore: String,
    val isPredefined: Boolean
)
