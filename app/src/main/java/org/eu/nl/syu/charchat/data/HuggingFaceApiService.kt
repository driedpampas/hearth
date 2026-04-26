package org.eu.nl.syu.charchat.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class HuggingFaceApiService @Inject constructor(
    private val authRepositoryProvider: Provider<AuthRepository>
) {
    private val gson = Gson()
    private val TAG = "HuggingFaceApiService"

    suspend fun <T> authorizedRequest(block: suspend (token: String) -> Pair<T?, Int>): T? {
        val authRepository = authRepositoryProvider.get()
        var token = authRepository.getAccessToken() ?: return null

        var (result, code) = block(token)

        if (code == 401) {
            Log.w(TAG, "Got 401, attempting refresh...")
            token = authRepository.refreshAccessToken() ?: return null
            val (retryResult, retryCode) = block(token)
            if (retryCode == 401) {
                Log.e(TAG, "Still 401 after refresh")
                authRepository.triggerAuthError(AuthError.RefreshFailed)
            }
            return retryResult
        }

        return result
    }

    suspend fun whoami(): UserInfo? = authorizedRequest { token ->
        fetchUserInfoInternal(token)
    }

    private suspend fun fetchUserInfoInternal(token: String): Pair<UserInfo?, Int> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Verifying identity via whoami...")
        try {
            val url = URL("https://huggingface.co/oauth/userinfo")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val reader = InputStreamReader(connection.inputStream)
                val userInfo = gson.fromJson(reader, UserInfo::class.java)
                return@withContext userInfo to responseCode
            }
            return@withContext null to responseCode
        } catch (e: Exception) {
            Log.e(TAG, "userinfo error during request", e)
        }
        null to -1
    }

    suspend fun checkModelAccess(author: String, modelId: String): AccessResult = authorizedRequest { token ->
        val (result, code) = checkModelAccessInternal(token, author, modelId)
        result to code
    } ?: AccessResult.Error("Unauthorized")

    private suspend fun checkModelAccessInternal(
        token: String,
        author: String,
        modelId: String
    ): Pair<AccessResult, Int> =
        withContext(Dispatchers.IO) {
            val fullId = "$author/$modelId"
            Log.d(TAG, "Checking access for gated model: $fullId")
            try {
                val url = URL("https://huggingface.co/api/models/$fullId")
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("Authorization", "Bearer $token")

                val responseCode = connection.responseCode
                val result = when (responseCode) {
                    200 -> AccessResult.Granted
                    401 -> AccessResult.Unauthorized
                    403 -> AccessResult.Forbidden
                    else -> AccessResult.Error("HTTP $responseCode")
                }
                return@withContext result to responseCode
            } catch (e: Exception) {
                Log.e(TAG, "Error while checking model access for $fullId", e)
                return@withContext AccessResult.Error(e.message ?: "Unknown error") to -1
            }
        }

    suspend fun fetchCommunityModels(author: String): List<HFModel> = authorizedRequest { token ->
        fetchCommunityModelsInternal(token, author)
    } ?: emptyList()

    private suspend fun fetchCommunityModelsInternal(token: String, author: String): Pair<List<HFModel>?, Int> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Indexing models from $author...")
            try {
                val url =
                    URL("https://huggingface.co/api/models?author=$author&library=litert-lm&full=true")
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("Authorization", "Bearer $token")

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val reader = InputStreamReader(connection.inputStream)
                    val models: Array<HFModel> = gson.fromJson(reader, Array<HFModel>::class.java)
                    return@withContext models.toList() to responseCode
                }
                return@withContext null to responseCode
            } catch (e: Exception) {
                Log.e(TAG, "Error during model indexing from $author", e)
            }
            null to -1
        }

    data class UserInfo(
        val sub: String,
        val name: String,
        @SerializedName("preferred_username") val preferredUsername: String,
        val profile: String?,
        val picture: String?,
        val website: String?,
        @SerializedName("isPro") val isPro: Boolean,
        val email: String? = null
    )

    sealed class AccessResult {
        object Granted : AccessResult()
        object Unauthorized : AccessResult()
        object Forbidden : AccessResult()
        data class Error(val message: String) : AccessResult()
    }

    data class HFModel(
        val id: String,
        val siblings: List<Sibling>? = null,
        val gated: Boolean = false,
        @SerializedName("pipeline_tag") val pipelineTag: String? = null,
        @SerializedName("library_name") val libraryName: String? = null
    )

    data class Sibling(
        val rfilename: String
    )

    data class OAuthTokenResponse(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("refresh_token") val refreshToken: String?,
        @SerializedName("expires_in") val expiresIn: Long,
        @SerializedName("token_type") val tokenType: String,
        val scope: String?
    )

    suspend fun refreshToken(refreshToken: String): OAuthTokenResponse? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Refreshing token...")
        try {
            val url = URL("https://huggingface.co/oauth/token")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true

            val clientId = org.eu.nl.syu.charchat.common.ProjectConfig.clientId
            val postData = "grant_type=refresh_token&refresh_token=$refreshToken&client_id=$clientId"

            connection.outputStream.use { os ->
                os.write(postData.toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val reader = InputStreamReader(connection.inputStream)
                val response = gson.fromJson(reader, OAuthTokenResponse::class.java)
                Log.d(TAG, "Token refresh successful")
                return@withContext response
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "Token refresh failed with HTTP $responseCode: $errorStream")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during token refresh", e)
        }
        null
    }
}
