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
import javax.inject.Singleton

@Singleton
class HuggingFaceApiService @Inject constructor() {
    private val gson = Gson()
    private val TAG = "HuggingFaceApiService"

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

    suspend fun whoami(token: String): UserInfo? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Verifying identity via whoami...")
        try {
            val url = URL("https://huggingface.co/oauth/userinfo")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val reader = InputStreamReader(connection.inputStream)
                val userInfo = gson.fromJson(reader, UserInfo::class.java)
                Log.d(TAG, "userinfo success: $userInfo")
                return@withContext userInfo
            } else {
                Log.e(TAG, "userinfo failed with HTTP $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "userinfo error during request", e)
        }
        null
    }

    suspend fun checkModelAccess(token: String, modelId: String): AccessResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Checking access for gated model: $modelId")
        try {
            val url = URL("https://huggingface.co/api/models/$modelId")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            return@withContext when (responseCode) {
                200 -> {
                    Log.d(TAG, "Access GRANTED for $modelId")
                    AccessResult.Granted
                }

                401 -> {
                    Log.w(TAG, "Access UNAUTHORIZED for $modelId: Token might be invalid.")
                    AccessResult.Unauthorized
                }

                403 -> {
                    Log.w(TAG, "Access FORBIDDEN for $modelId: User may need to accept gating agreement.")
                    AccessResult.Forbidden
                }

                else -> {
                    Log.e(TAG, "Access check for $modelId returned unexpected code: $responseCode")
                    AccessResult.Error("HTTP $responseCode")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while checking model access for $modelId", e)
            AccessResult.Error(e.message ?: "Unknown error")
        }
    }

    data class HFModel(
        val id: String,
        val siblings: List<Sibling>? = null,
        val gated: Boolean = false,
        @SerializedName("pipeline_tag") val pipelineTag: String? = null
    )

    data class Sibling(
        val rfilename: String
    )

    suspend fun fetchCommunityModels(token: String, author: String): List<HFModel> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Indexing models from $author...")
        try {
            val url =
                URL("https://huggingface.co/api/models?author=$author&pipeline_tag=text-generation&full=True")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val reader = InputStreamReader(connection.inputStream)
                val models: Array<HFModel> = gson.fromJson(reader, Array<HFModel>::class.java)
                Log.d(TAG, "Successfully indexed ${models.size} models from $author")
                Log.d(TAG, "Indexed models: ${models.joinToString { it.id }}")
                return@withContext models.toList()
            } else {
                Log.e(TAG, "Failed to index models from $author: HTTP $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during model indexing from $author", e)
        }
        emptyList()
    }
}
