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

package org.eu.nl.syu.hearth.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification

/**
 * Data class to represent search results from the virtual vec0 table.
 */
data class VectorSearchResult(
    val rowId: Long,
    val distance: Float
)

@Dao
interface VectorDao {

    /**
     * Insert embedding for a lore chunk into the vec_lore table.
     */
    @SkipQueryVerification
    @Query("INSERT INTO vec_lore(lore_id, embedding) VALUES (:loreId, :embedding)")
    suspend fun insertLoreEmbedding(loreId: String, embedding: ByteArray)

    /**
     * Insert embedding for a memory entry into the vec_memory table.
     */
    @SkipQueryVerification
    @Query("INSERT INTO vec_memory(memory_id, embedding) VALUES (:memoryId, :embedding)")
    suspend fun insertMemoryEmbedding(memoryId: String, embedding: ByteArray)

    /**
     * KNN search for lore chunks. Returns the actual lore chunks.
     */
    @SkipQueryVerification
    @Query("""
        SELECT lc.* 
        FROM lore_chunks lc
        INNER JOIN vec_lore vl ON lc.id = vl.lore_id
        WHERE vl.embedding MATCH :queryEmbedding
          AND vl.k = :topK
        ORDER BY vl.distance ASC
    """)
    suspend fun searchLoreChunks(queryEmbedding: ByteArray, topK: Int = 5): List<LoreChunkEntity>

    /**
     * KNN search for memory entries. Returns the actual memory entries.
     */
    @SkipQueryVerification
    @Query("""
        SELECT me.* 
        FROM memory_entries me
        INNER JOIN vec_memory vm ON me.id = vm.memory_id
        WHERE vm.embedding MATCH :queryEmbedding
          AND vm.k = :topK
        ORDER BY vm.distance ASC
    """)
    suspend fun searchMemoryEntries(queryEmbedding: ByteArray, topK: Int = 5): List<MemoryEntryEntity>

    @SkipQueryVerification
    @Query("DELETE FROM vec_lore")
    suspend fun clearLoreVectors()

    @SkipQueryVerification
    @Query("DELETE FROM vec_memory")
    suspend fun clearMemoryVectors()

    @SkipQueryVerification
    @Query("DELETE FROM vec_lore WHERE lore_id IN (SELECT id FROM lore_chunks WHERE characterId = :characterId)")
    suspend fun deleteLoreVectorsForCharacter(characterId: String)

    @SkipQueryVerification
    @Query("DELETE FROM vec_memory WHERE memory_id = :memoryId")
    suspend fun deleteMemoryVector(memoryId: String)
}
