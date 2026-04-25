package org.eu.nl.syu.charchat.common

import androidx.core.net.toUri
import net.openid.appauth.AuthorizationServiceConfiguration
import org.eu.nl.syu.charchat.BuildConfig

object ProjectConfig {
    // Hugging Face Client ID.
    val clientId = BuildConfig.HF_CLIENT_ID

    // Registered redirect URI.
    const val redirectUri = "org.eu.nl.syu.charchat://oauth"

    // OAuth 2.0 Endpoints
    private const val authEndpoint = "https://huggingface.co/oauth/authorize"
    private const val tokenEndpoint = "https://huggingface.co/oauth/token"

    // OAuth service configuration
    val authServiceConfig = AuthorizationServiceConfiguration(
        authEndpoint.toUri(),
        tokenEndpoint.toUri()
    )
}
