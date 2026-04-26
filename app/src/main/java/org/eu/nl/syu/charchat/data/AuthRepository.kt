package org.eu.nl.syu.charchat.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMs: Long
)

sealed class AuthError {
    object RefreshFailed : AuthError()
}

@Singleton
class AuthRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val hfApiService: Provider<HuggingFaceApiService>
) {
    private val ACCESS_TOKEN = stringPreferencesKey("hf_access_token")
    private val REFRESH_TOKEN = stringPreferencesKey("hf_refresh_token")
    private val EXPIRES_AT = longPreferencesKey("hf_expires_at")

    private val _authError = MutableStateFlow<AuthError?>(null)
    val authError: StateFlow<AuthError?> = _authError.asStateFlow()

    private val refreshMutex = Mutex()

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
        _authError.value = null
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
            preferences.remove(EXPIRES_AT)
        }
        _authError.value = null
    }

    fun triggerAuthError(error: AuthError) {
        _authError.value = error
    }

    fun clearAuthError() {
        _authError.value = null
    }

    suspend fun getAccessToken(): String? {
        val token = authToken.first() ?: return null
        
        // Check if expired (with 5 min buffer)
        if (System.currentTimeMillis() < token.expiresAtMs - 5 * 60 * 1000) {
            return token.accessToken
        }

        // Try to refresh
        return refreshAccessToken()
    }

    suspend fun refreshAccessToken(): String? = refreshMutex.withLock {
        val token = authToken.first() ?: return null
        
        // Check if another thread already refreshed it while we were waiting for the lock
        if (System.currentTimeMillis() < token.expiresAtMs - 5 * 60 * 1000) {
            return token.accessToken
        }

        Log.d("AuthRepository", "Refreshing access token...")
        val response = hfApiService.get().refreshToken(token.refreshToken)
        if (response != null) {
            val newToken = AuthToken(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken ?: token.refreshToken, // Use old one if not provided
                expiresAtMs = System.currentTimeMillis() + (response.expiresIn * 1000)
            )
            saveToken(newToken)
            return newToken.accessToken
        } else {
            Log.e("AuthRepository", "Failed to refresh access token")
            triggerAuthError(AuthError.RefreshFailed)
            return null
        }
    }
}
