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
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    private val SELECTED_EMBEDDING_MODEL = stringPreferencesKey("selected_embedding_model")
    private val COMMUNITY_AUTHORS = stringSetPreferencesKey("community_authors")
    private val EXPERIMENTAL_NPU_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("experimental_npu_enabled")
    private val MODEL_BACKEND_CACHE = stringPreferencesKey("model_backend_cache")
    private val PREFERRED_BACKEND = stringPreferencesKey("preferred_backend")
    private val DEFAULT_MAX_TOKENS = androidx.datastore.preferences.core.intPreferencesKey("default_max_tokens")

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
            gson.fromJson(cacheJson, Map::class.java) as? Map<String, String>
        } catch (e: Exception) {
            null
        }
        return cache?.get(modelHash)
    }

    suspend fun setCachedBackend(modelHash: String, backend: String) {
        context.modelDataStore.edit { preferences ->
            val currentJson = preferences[MODEL_BACKEND_CACHE]
            val currentCache = try {
                if (currentJson != null) gson.fromJson(currentJson, Map::class.java) as? MutableMap<String, String> else mutableMapOf()
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

    fun getSoc(): String {
        return Build.SOC_MODEL
            .lowercase()
    }

    suspend fun getAvailableModels(): List<AllowedModel> {
        val models = mutableListOf<AllowedModel>()
        
        // 1. Add EmbeddingGemma-300m manually
        models.add(getEmbeddingGemmaModel())

        // 2. Fetch from Hugging Face if token is available
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
                            // Look for .litertlm first (LLM container), fallback to .tflite
                            val modelFile = hfModel.siblings?.find { it.rfilename.endsWith(".litertlm") }?.rfilename
                                ?: hfModel.siblings?.find { it.rfilename.endsWith(".tflite") }?.rfilename

                            if (modelFile != null) {
                                models.add(AllowedModel(
                                    name = hfModel.id.substringAfter("/"),
                                    modelId = hfModel.id,
                                    author = author,
                                    modelFile = modelFile,
                                    commitHash = "main",
                                    description = "Community model from $author.",
                                    sizeInBytes = 0,
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

        return models
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
}

