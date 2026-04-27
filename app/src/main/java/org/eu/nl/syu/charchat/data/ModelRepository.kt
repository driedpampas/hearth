package org.eu.nl.syu.charchat.data

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.modelDataStore: DataStore<Preferences> by preferencesDataStore(name = "model_prefs")

data class SocModelFile(
    @SerializedName("modelFile") val modelFile: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("commitHash") val commitHash: String?,
    @SerializedName("sizeInBytes") val sizeInBytes: Long?
)

data class AllowedModel(
    val name: String,
    val modelId: String,
    val modelFile: String,
    val commitHash: String,
    val description: String,
    val sizeInBytes: Long,
    val author: String? = null,
    val url: String? = null,
    val socToModelFiles: Map<String, SocModelFile>? = null,
    val taskTypes: List<String> = emptyList()
)

@Singleton
class ModelRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val hfApiService: HuggingFaceApiService,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()
    private val socFilenameRegex = Regex("""(?:^|[._-])(sm\d{4}|mt\d{4})(?:[._-]|$)""", RegexOption.IGNORE_CASE)
    private val SELECTED_EMBEDDING_MODEL = stringPreferencesKey("selected_embedding_model")
    private val COMMUNITY_AUTHORS = stringSetPreferencesKey("community_authors")
    private val EXPERIMENTAL_NPU_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("experimental_npu_enabled")
    private val MODEL_BACKEND_CACHE = stringPreferencesKey("model_backend_cache")
    private val PREFERRED_BACKEND = stringPreferencesKey("preferred_backend")
    private val DEFAULT_MAX_TOKENS = androidx.datastore.preferences.core.intPreferencesKey("default_max_tokens")
    private val BLACKLISTED_MODEL_HASHES = stringSetPreferencesKey("blacklisted_model_hashes")

    val selectedEmbeddingModel: Flow<String?> = context.modelDataStore.data.map { preferences ->
        preferences[SELECTED_EMBEDDING_MODEL]
    }

    val communityAuthors: Flow<Set<String>> = context.modelDataStore.data.map { preferences ->
        preferences[COMMUNITY_AUTHORS] ?: emptySet()
    }

    suspend fun addCommunityAuthor(author: String) {
        context.modelDataStore.edit { preferences ->
            val current = preferences[COMMUNITY_AUTHORS] ?: emptySet()
            preferences[COMMUNITY_AUTHORS] = current + author
        }
    }

    suspend fun removeCommunityAuthor(author: String) {
        context.modelDataStore.edit { preferences ->
            val current = preferences[COMMUNITY_AUTHORS] ?: emptySet()
            preferences[COMMUNITY_AUTHORS] = current - author
        }
    }

    suspend fun setSelectedEmbeddingModel(name: String) {
        context.modelDataStore.edit { preferences ->
            preferences[SELECTED_EMBEDDING_MODEL] = name
        }
    }

    val experimentalNpuEnabled: Flow<Boolean> = context.modelDataStore.data.map { preferences ->
        preferences[EXPERIMENTAL_NPU_ENABLED] ?: false
    }

    suspend fun setExperimentalNpuEnabled(enabled: Boolean) {
        context.modelDataStore.edit { preferences ->
            preferences[EXPERIMENTAL_NPU_ENABLED] = enabled
        }
    }

    suspend fun getCachedBackend(modelHash: String): String? {
        val cacheJson = context.modelDataStore.data.first()[MODEL_BACKEND_CACHE] ?: return null
        val cache = try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(cacheJson, type)
        } catch (e: Exception) {
            null
        }
        return cache?.get(modelHash)
    }

    suspend fun setCachedBackend(modelHash: String, backend: String) {
        context.modelDataStore.edit { preferences ->
            val currentJson = preferences[MODEL_BACKEND_CACHE]
            val currentCache = try {
                val type = object : TypeToken<MutableMap<String, String>>() {}.type
                if (currentJson != null) gson.fromJson<MutableMap<String, String>>(currentJson, type) else mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            } ?: mutableMapOf()
            
            currentCache[modelHash] = backend
            preferences[MODEL_BACKEND_CACHE] = gson.toJson(currentCache)
        }
    }

    val preferredBackend: Flow<String> = context.modelDataStore.data.map { preferences ->
        preferences[PREFERRED_BACKEND] ?: "Automatic"
    }

    suspend fun setPreferredBackend(backend: String) {
        context.modelDataStore.edit { preferences ->
            preferences[PREFERRED_BACKEND] = backend
        }
    }

    val defaultMaxTokens: Flow<Int> = context.modelDataStore.data.map { preferences ->
        preferences[DEFAULT_MAX_TOKENS] ?: 4096
    }

    suspend fun setDefaultMaxTokens(maxTokens: Int) {
        context.modelDataStore.edit { preferences ->
            preferences[DEFAULT_MAX_TOKENS] = maxTokens
        }
    }

    suspend fun isModelBlacklisted(hash: String): Boolean {
        val set: Set<String> = context.modelDataStore.data.first()[BLACKLISTED_MODEL_HASHES] ?: emptySet()
        return set.contains(hash)
    }

    suspend fun blacklistModel(hash: String) {
        context.modelDataStore.edit { preferences ->
            val current = preferences[BLACKLISTED_MODEL_HASHES] ?: emptySet<String>()
            preferences[BLACKLISTED_MODEL_HASHES] = current.toMutableSet().apply { add(hash) }
        }
    }

    suspend fun quarantineModel(file: File) {
        val hash = hashOf(file)
        if (hash.isNotEmpty()) {
            blacklistModel(hash)
        }
        if (file.exists()) {
            file.delete()
        }
    }

    fun hashOf(file: File): String {
        if (!file.exists()) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            file.inputStream().use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: IOException) {
            ""
        }
    }

    fun getSoc(): String {
        return Build.SOC_MODEL
            .lowercase()
    }

    suspend fun getAvailableModels(): List<AllowedModel> {
        val models = mutableListOf<AllowedModel>()

        // Fetch from Hugging Face if token is available
        try {
            val token = authRepository.getAccessToken()
            if (token != null) {
                val authorsFlow = context.modelDataStore.data.map { preferences ->
                    preferences[COMMUNITY_AUTHORS] ?: setOf("litert-community")
                }
                val authors = authorsFlow.first().ifEmpty { setOf("litert-community") }
                
                authors.forEach { author ->
                    Log.d("ModelRepository", "Fetching models from author: $author")
                    val hfModels = hfApiService.fetchCommunityModels(author)
                    hfModels.forEach { hfModel ->
                        // Only include models with library_name "litert-lm" (primary filter)
                        if (hfModel.libraryName == "litert-lm") {
                            val siblings = hfModel.siblings.orEmpty()
                            val socToModelFiles = siblings.mapNotNull { sibling ->
                                extractSocTag(sibling.rfilename)?.let { soc ->
                                    soc to SocModelFile(
                                        modelFile = sibling.rfilename,
                                        url = null,
                                        commitHash = "main",
                                        sizeInBytes = null
                                    )
                                }
                            }.toMap()

                            // Prefer a generic file as the default, then fall back to any available file.
                            val modelFile = siblings
                                .firstOrNull { it.rfilename.endsWith(".litertlm") && extractSocTag(it.rfilename) == null }
                                ?.rfilename
                                ?: siblings.firstOrNull { it.rfilename.endsWith(".tflite") && extractSocTag(it.rfilename) == null }?.rfilename
                                ?: siblings.firstOrNull { it.rfilename.endsWith(".litertlm") }?.rfilename
                                ?: siblings.firstOrNull { it.rfilename.endsWith(".tflite") }?.rfilename

                            if (modelFile != null) {
                                models.add(AllowedModel(
                                    name = hfModel.id.substringAfter("/"),
                                    modelId = hfModel.id,
                                    author = author,
                                    modelFile = modelFile,
                                    commitHash = "main",
                                    description = "Community model from $author.",
                                    sizeInBytes = 0,
                                    socToModelFiles = socToModelFiles.ifEmpty { null },
                                    taskTypes = if (hfModel.pipelineTag != null) listOf(hfModel.pipelineTag) else emptyList()
                                ))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ModelRepository", "Error fetching dynamic models", e)
        }

        // Keep normal chat models ahead of embedding models.
        return models.sortedWith(
            compareBy<AllowedModel> { it.taskTypes.contains("embedding") }
                .thenBy { it.name.lowercase() }
        ) + getEmbeddingGemmaModel()
    }

    private fun getEmbeddingGemmaModel(): AllowedModel {
        val modelId = "litert-community/embeddinggemma-300m"
        val commitHash = "main" // Or specific hash if known
        
        // SOC specific files based on HF tree
        val socToModelFiles = mapOf(
            "mt6991" to SocModelFile("embeddinggemma-300M_seq1024_mixed-precision.mediatek.mt6991.tflite", null, commitHash, 188L * 1024 * 1024),
            "mt6993" to SocModelFile("embeddinggemma-300M_seq1024_mixed-precision.mediatek.mt6993.tflite", null, commitHash, 186L * 1024 * 1024),
            "sm8550" to SocModelFile("embeddinggemma-300M_seq1024_mixed-precision.qualcomm.sm8550.tflite", null, commitHash, 195L * 1024 * 1024),
            "sm8650" to SocModelFile("embeddinggemma-300M_seq1024_mixed-precision.qualcomm.sm8650.tflite", null, commitHash, 195L * 1024 * 1024),
            "sm8750" to SocModelFile("embeddinggemma-300M_seq1024_mixed-precision.qualcomm.sm8750.tflite", null, commitHash, 195L * 1024 * 1024),
            "sm8850" to SocModelFile("embeddinggemma-300M_seq1024_mixed-precision.qualcomm.sm8850.tflite", null, commitHash, 199L * 1024 * 1024)
        )

        return AllowedModel(
            name = "EmbeddingGemma-300m",
            modelId = modelId,
            author = "litert-community",
            modelFile = "embeddinggemma-300M_seq1024_mixed-precision.tflite",
            commitHash = commitHash,
            description = "High-performance text embedding model based on Gemma architecture.",
            sizeInBytes = 183L * 1024 * 1024,
            socToModelFiles = socToModelFiles,
            taskTypes = listOf("embedding")
        )
    }

    fun getDownloadUrl(model: AllowedModel): String {
        val soc = getSoc()
        Log.d("ModelRepository", "Determining download URL for model: ${model.name}, detected SOC: $soc")
        
        val socEntry = model.socToModelFiles?.entries?.find { soc.contains(it.key) }
        val socFile = socEntry?.value
        
        if (socFile != null) {
            Log.i("ModelRepository", "Matched SOC variant: ${socEntry.key} for $soc")
        } else {
            Log.w("ModelRepository", "No SOC-specific variant found for $soc in ${model.socToModelFiles?.keys}. Falling back to default: ${model.modelFile}")
        }
        
        val fileName = socFile?.modelFile ?: model.modelFile
        val hash = socFile?.commitHash ?: model.commitHash
        
        val url = socFile?.url ?: "https://huggingface.co/${model.modelId}/resolve/$hash/$fileName?download=true"
        Log.d("ModelRepository", "Final download URL: $url")
        return url
    }

    fun getDownloadFileName(model: AllowedModel): String {
        val soc = getSoc()
        val socFile = model.socToModelFiles?.entries?.find { soc.contains(it.key) }?.value
        return socFile?.modelFile ?: model.modelFile
    }

    private fun extractSocTag(fileName: String): String? {
        return socFilenameRegex.find(fileName)?.groupValues?.getOrNull(1)?.lowercase()
    }
}
