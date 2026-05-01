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

package org.eu.nl.syu.hearth.runtime

import android.content.Context
import com.google.ai.edge.localagents.rag.models.EmbedData
import com.google.ai.edge.localagents.rag.models.Embedder
import com.google.ai.edge.localagents.rag.models.EmbeddingRequest
import com.google.ai.edge.localagents.rag.models.GemmaEmbeddingModel
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.eu.nl.syu.hearth.data.ModelManager
import org.eu.nl.syu.hearth.data.ModelRepository
import org.eu.nl.syu.hearth.data.local.VectorDao
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class EmbeddingEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val vectorDao: VectorDao,
    private val modelRepository: ModelRepository,
    private val modelManager: ModelManager
) {
    private var embedder: Embedder<String>? = null
    private var currentModelPath: String? = null
    private val mutex = Mutex()
    private val callbackExecutor = Executors.newSingleThreadExecutor()

    /**
     * Ensures the embedder is initialized with the currently selected embedding model.
     */
    suspend fun ensureInitialized() = mutex.withLock {
        val selectedModelName = modelRepository.selectedEmbeddingModel.first() ?: "EmbeddingGemma-300m"
        val availableModels = modelRepository.getAvailableModels()
        val model = availableModels.find { it.name == selectedModelName } ?: return@withLock
        val fileName = modelRepository.getDownloadFileName(model)
        
        val file = File(context.filesDir, "models/$fileName")
        if (file.exists() && file.absolutePath != currentModelPath) {
            initialize(file.absolutePath)
        }
    }

    private suspend fun initialize(modelPath: String) {
        withContext(Dispatchers.IO) {
            val useNpu = modelRepository.experimentalNpuEnabled.first()
            try {
                embedder = GemmaEmbeddingModel(
                    modelPath,
                    context.cacheDir.path,
                    useNpu
                )
                currentModelPath = modelPath
            } catch (e: Exception) {
                // Fallback to CPU if NPU fails or if was already using CPU and failed (unlikely)
                embedder = GemmaEmbeddingModel(
                    modelPath,
                    context.cacheDir.path,
                    false // isNpu
                )
                currentModelPath = modelPath
            }
        }
    }

    /**
     * Generates a vector for the given text.
     * Truncates to 256 dims and returns a Little-Endian ByteArray.
     */
    suspend fun getVector(text: String, isQuery: Boolean): ByteArray = withContext(Dispatchers.Default) {
        ensureInitialized()
        val currentEmbedder = embedder ?: return@withContext ByteArray(256 * 4)

        // GEMMA Embedding expects TaskType
        val taskType = if (isQuery) EmbedData.TaskType.RETRIEVAL_QUERY else EmbedData.TaskType.RETRIEVAL_DOCUMENT
        val embedData = EmbedData.create(text, taskType, isQuery)
        val request = EmbeddingRequest.create(listOf(embedData))
        
        val future = currentEmbedder.getEmbeddings(request)
        val rawList = future.await()
        val rawArray = FloatArray(rawList.size) { rawList[it] }
        
        VectorUtils.processEmbedding(rawArray)
    }



    /**
     * Cleans up the embedder resources.
     */
    fun close() {
        // GemmaEmbeddingModel in 0.3.0 doesn't seem to have a close method in the javap output,
        // but it might implement AutoCloseable.
        (embedder as? AutoCloseable)?.close()
        embedder = null
        currentModelPath = null
    }

    // Helper to await ListenableFuture
    private suspend fun <T> ListenableFuture<T>.await(): T = suspendCoroutine { cont ->
        Futures.addCallback(this, object : FutureCallback<T> {
            override fun onSuccess(result: T) {
                cont.resume(result)
            }
            override fun onFailure(t: Throwable) {
                cont.resumeWithException(t)
            }
        }, callbackExecutor)
    }
}
