/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.domain

import org.eu.nl.syu.hearth.data.Character

/**
 * Logic to replace identity placeholders in character text.
 */
object TemplateProcessor {

    /**
     * Replaces {{char}}, {{user}}, and {{tagline}} in the given text.
     */
    fun process(
        text: String,
        character: Character,
        userName: String = "User",
        userBio: String? = null
    ): String {
        return text.replace("{{char}}", character.name)
            .replace("{{user}}", userName)
            .replace("{{persona}}", userBio ?: "")
            .replace("{{tagline}}", character.tagline)
    }
}
