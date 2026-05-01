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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.eu.nl.syu.hearth.data.ModelManager
import org.eu.nl.syu.hearth.data.ModelRepository
import org.eu.nl.syu.hearth.data.local.VectorDao
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbeddingEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val vectorDao: VectorDao,
    private val modelRepository: ModelRepository,
    private val modelManager: ModelManager
) {
    private var modelFile: File? = null
    // Note: If you are using Google Play Services TFLite, you'd initialize InterpreterApi here.
    // private var interpreter: InterpreterApi? = null

    suspend fun ensureInitialized() {
        val selectedModelName = modelRepository.selectedEmbeddingModel.first() ?: return
        val availableModels = modelRepository.getAvailableModels()
        val model = availableModels.find { it.name == selectedModelName } ?: return
        val fileName = modelRepository.getDownloadFileName(model)
        
        val file = File(context.filesDir, "models/$fileName")
        if (file.exists() && file != modelFile) {
            initialize(file)
        }
    }

    suspend fun initialize(file: File) {
        withContext(Dispatchers.IO) {
            modelFile = file
            // TODO: Initialize TFLite Interpreter here
            // interpreter = Interpreter(modelFile!!)
        }
    }

    suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        // TODO: Replace with actual inference using `interpreter?.run(...)` or LiteRT.
        // This is a placeholder generating a dummy 256-dimensional embedding.
        val dummyEmbedding = FloatArray(256) { 0.0f }
        // Simple hash-based dummy to make it deterministic
        val hash = text.hashCode().toFloat()
        dummyEmbedding[0] = hash
        
        // Normalize vector for cosine similarity
        var norm = 0f
        for (v in dummyEmbedding) norm += v * v
        norm = kotlin.math.sqrt(norm)
        if (norm > 0) {
            for (i in dummyEmbedding.indices) dummyEmbedding[i] /= norm
        }
        
        dummyEmbedding
    }

    /**
     * Converts a FloatArray into a Little-Endian ByteArray for sqlite-vec.
     */
    fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(floatArray.size * 4)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for (value in floatArray) {
            byteBuffer.putFloat(value)
        }
        return byteBuffer.array()
    }

    suspend fun similaritySearch(query: String, topK: Int = 3): List<String> {
        ensureInitialized()
        val queryEmbedding = generateEmbedding(query)
        val queryBytes = floatArrayToByteArray(queryEmbedding)

        // The VectorDao searchLoreChunks method joins vec_lore with lore_chunks
        val results = vectorDao.searchLoreChunks(queryBytes, topK)
        return results.map { it.text }
    }
}
