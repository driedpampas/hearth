/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.data

/**
 * Visual identity for a chat thread.
 */
data class ChatStyle(
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
    val backgroundColor: String? = null,
    val fontName: String? = null,
    val backgroundBlur: Float = 0f,
    val overlayOpacity: Float = 0.5f
)
