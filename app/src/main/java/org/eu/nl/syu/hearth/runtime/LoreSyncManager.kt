/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.eu.nl.syu.hearth.data.Character
import org.eu.nl.syu.hearth.data.local.AppDatabase
import org.eu.nl.syu.hearth.data.local.LoreChunkEntity
import org.eu.nl.syu.hearth.domain.LoreSplitter
import org.eu.nl.syu.hearth.domain.TemplateProcessor
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoreSyncManager @Inject constructor(
    private val db: AppDatabase,
    private val embeddingEngine: EmbeddingEngine,
    private val loreSplitter: LoreSplitter
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        data class Error(val message: String) : SyncState()
    }

    suspend fun isSynced(characterId: String, threadId: String?): Boolean = withContext(Dispatchers.IO) {
        if (threadId == null) {
            val chunkCount = db.loreChunkDao().getGlobalChunkCount(characterId)
            if (chunkCount == 0) return@withContext true
            val vectorCount = db.vectorDao().getGlobalLoreVectorCount(characterId)
            vectorCount >= chunkCount
        } else {
            val chunkCount = db.loreChunkDao().getThreadChunkCount(threadId)
            if (chunkCount == 0) return@withContext true
            val vectorCount = db.vectorDao().getThreadLoreVectorCount(threadId)
            vectorCount >= chunkCount
        }
    }

    /**
     * Re-chunks and re-embeds lore for a character or specific thread.
     * Handles templating {{char}} and {{user}} before embedding.
     */
    suspend fun syncLore(
        character: Character, 
        loreText: String, 
        threadId: String? = null,
        userName: String = "User"
    ) = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing
        try {
            // 1. Invalidate old data based on scope
            if (threadId == null) {
                db.vectorDao().deleteLoreVectorsForCharacter(character.id)
                db.loreChunkDao().deleteGlobalChunksForCharacter(character.id)
            } else {
                db.vectorDao().deleteLoreVectorsForThread(threadId)
                db.loreChunkDao().deleteChunksForThread(threadId)
            }

            // 2. Chunk lore
            val chunks: List<String> = loreSplitter.splitLore(loreText)
            val chunkEntities = chunks.map { text ->
                // Resolve templates before embedding
                val processedText = TemplateProcessor.process(text, character, userName)
                LoreChunkEntity(
                    id = UUID.randomUUID().toString(),
                    characterId = character.id,
                    text = processedText,
                    threadId = threadId
                )
            }

            // 3. Embed and save
            db.loreChunkDao().insertChunks(chunkEntities)
            
            for (chunk in chunkEntities) {
                val vector = embeddingEngine.getVector(chunk.text, isQuery = false)
                db.vectorDao().insertLoreEmbedding(chunk.id, vector)
            }

            _syncState.value = SyncState.Idle
        } catch (e: Exception) {
            e.printStackTrace()
            _syncState.value = SyncState.Error(e.message ?: "Unknown error during lore sync")
        }
    }

    fun clearError() {
        if (_syncState.value is SyncState.Error) {
            _syncState.value = SyncState.Idle
        }
    }
}
