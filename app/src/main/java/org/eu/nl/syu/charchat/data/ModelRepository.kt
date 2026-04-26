package org.eu.nl.syu.charchat.data

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

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
    val url: String? = null,
    val socToModelFiles: Map<String, SocModelFile>? = null,
    val taskTypes: List<String> = emptyList()
)

@Singleton
class ModelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hfApiService: HuggingFaceApiService,
    private val authRepository: AuthRepository
) {
    private val gson = Gson()
    private val SELECTED_EMBEDDING_MODEL = stringPreferencesKey("selected_embedding_model")
    private val COMMUNITY_AUTHORS = stringSetPreferencesKey("community_authors")

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

    fun getSoc(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL ?: ""
        } else {
            ""
        }.lowercase()
    }

    suspend fun getAvailableModels(): List<AllowedModel> {
        val models = mutableListOf<AllowedModel>()
        
        // 1. Load from assets (Gallery models)
        try {
            val json = context.assets.open("model_allowlist.json").bufferedReader().use { it.readText() }
            val allowlist = gson.fromJson(json, ModelAllowlist::class.java)
            models.addAll(allowlist.models)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Add EmbeddingGemma-300m manually
        models.add(getEmbeddingGemmaModel())

        // 3. Fetch from Hugging Face if token is available
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
                        val tfliteFile = hfModel.siblings?.find { it.rfilename.endsWith(".tflite") }?.rfilename
                        if (tfliteFile != null) {
                            models.add(AllowedModel(
                                name = hfModel.id.substringAfter("/"),
                                modelId = hfModel.id,
                                modelFile = tfliteFile,
                                commitHash = "main",
                                description = "Community model from $author.",
                                sizeInBytes = 0,
                                taskTypes = if (hfModel.pipelineTag != null) listOf(hfModel.pipelineTag) else emptyList()
                            ))
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
        val socFile = model.socToModelFiles?.entries?.find { soc.contains(it.key) }?.value
        
        val fileName = socFile?.modelFile ?: model.modelFile
        val hash = socFile?.commitHash ?: model.commitHash
        
        return socFile?.url ?: "https://huggingface.co/${model.modelId}/resolve/$hash/$fileName?download=true"
    }

    fun getDownloadFileName(model: AllowedModel): String {
        val soc = getSoc()
        val socFile = model.socToModelFiles?.entries?.find { soc.contains(it.key) }?.value
        return socFile?.modelFile ?: model.modelFile
    }
}

data class ModelAllowlist(
    val models: List<AllowedModel>
)
