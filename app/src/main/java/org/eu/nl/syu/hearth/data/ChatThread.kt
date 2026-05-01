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

data class ChatThread(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = createdAt,
    val sequenceId: Int = 0,
    val styleJson: String? = null,
    val threadLore: String? = null
)
