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

package org.eu.nl.syu.hearth.common

import androidx.core.net.toUri
import net.openid.appauth.AuthorizationServiceConfiguration
import org.eu.nl.syu.hearth.BuildConfig

object ProjectConfig {
    // Hugging Face Client ID.
    val clientId = BuildConfig.HF_CLIENT_ID

    // Registered redirect URI.
    const val redirectUri = "org.eu.nl.syu.hearth://oauth"

    // OAuth 2.0 Endpoints
    private const val authEndpoint = "https://huggingface.co/oauth/authorize"
    private const val tokenEndpoint = "https://huggingface.co/oauth/token"

    // OAuth service configuration
    val authServiceConfig = AuthorizationServiceConfiguration(
        authEndpoint.toUri(),
        tokenEndpoint.toUri()
    )
}
