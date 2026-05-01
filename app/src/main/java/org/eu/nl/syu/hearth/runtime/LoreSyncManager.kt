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

    /**
     * Re-chunks and re-embeds lore for a character.
     * This should be called when character lore is edited.
     */
    suspend fun syncLore(character: Character, loreText: String) = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing
        try {
            // 1. Invalidate old data
            db.vectorDao().deleteLoreVectorsForCharacter(character.id)
            db.loreChunkDao().deleteChunksForCharacter(character.id)

            // 2. Chunk lore
            val chunks: List<String> = loreSplitter.splitLore(loreText)
            val chunkEntities = chunks.map { text ->
                LoreChunkEntity(
                    id = UUID.randomUUID().toString(),
                    characterId = character.id,
                    text = text
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
}
