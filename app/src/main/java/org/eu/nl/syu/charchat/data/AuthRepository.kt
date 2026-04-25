package org.eu.nl.syu.charchat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMs: Long
)

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ACCESS_TOKEN = stringPreferencesKey("hf_access_token")
    private val REFRESH_TOKEN = stringPreferencesKey("hf_refresh_token")
    private val EXPIRES_AT = longPreferencesKey("hf_expires_at")

    val authToken: Flow<AuthToken?> = context.dataStore.data.map { preferences ->
        val accessToken = preferences[ACCESS_TOKEN]
        val refreshToken = preferences[REFRESH_TOKEN]
        val expiresAt = preferences[EXPIRES_AT]

        if (accessToken != null && refreshToken != null && expiresAt != null) {
            AuthToken(accessToken, refreshToken, expiresAt)
        } else {
            null
        }
    }

    suspend fun saveToken(token: AuthToken) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = token.accessToken
            preferences[REFRESH_TOKEN] = token.refreshToken
            preferences[EXPIRES_AT] = token.expiresAtMs
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
            preferences.remove(EXPIRES_AT)
        }
    }

    suspend fun getAccessToken(): String? {
        val token = authToken.first()
        if (token != null) {
            // Check if expired (with 5 min buffer)
            if (System.currentTimeMillis() < token.expiresAtMs - 5 * 60 * 1000) {
                return token.accessToken
            }
        }
        return null
    }
}
