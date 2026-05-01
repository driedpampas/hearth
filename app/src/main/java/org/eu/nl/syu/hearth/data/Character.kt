/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.eu.nl.syu.hearth.data

import java.util.UUID

/**
 * Expanded Character model for immersive roleplay.
 */
data class Character(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tagline: String,
    val avatarUrl: String? = null,
    val roleInstruction: String,
    val reminderMessage: String = "",
    val initialMessages: List<ChatMessage> = emptyList(),
    val modelReference: String, // Path or ID of the .litertlm file
    val temp: Float = 1.0f,
    val topP: Float = 0.95f,
    val topK: Int = 64,
    val enableThinking: Boolean = false,
    val enableThinkingCompatibility: Boolean = false,
    val thinkingCompatibilityToken: String = "",
    val includeThinkingInContext: Boolean = false,
    val knowledgeBase: String = "",
    val sceneBackgroundUrl: String? = null,
    val isPredefined: Boolean = false,
    val lastUsedAt: Long = 0L
)
