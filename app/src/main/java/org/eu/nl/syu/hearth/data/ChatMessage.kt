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
