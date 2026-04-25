package org.eu.nl.syu.charchat.runtime

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.eu.nl.syu.charchat.data.local.VectorDao
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class EmbeddingEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vectorDao: VectorDao
) {
    // Note: If you are using Google Play Services TFLite, you'd initialize InterpreterApi here.
    // private var interpreter: InterpreterApi? = null

    suspend fun initialize(modelFile: File) {
        withContext(Dispatchers.IO) {
            // interpreter = InterpreterApi.create(modelFile, InterpreterApi.Options())
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
        val queryEmbedding = generateEmbedding(query)
        val queryBytes = floatArrayToByteArray(queryEmbedding)

        // The VectorDao searchLoreChunks method joins vec_lore with lore_chunks
        val results = vectorDao.searchLoreChunks(queryBytes, topK)
        return results.map { it.text }
    }
}
