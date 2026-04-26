package org.eu.nl.syu.charchat.data.local

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
     * KNN search for lore chunks. Returns the rowid and distance.
     */
    @SkipQueryVerification
    @Query("""
        SELECT rowid as rowId, distance
        FROM vec_lore
        WHERE embedding MATCH :queryEmbedding
        AND k = :topK
        ORDER BY distance ASC
    """)
    suspend fun searchNearestLore(queryEmbedding: ByteArray, topK: Int = 3): List<VectorSearchResult>

    /**
     * Given the search results, retrieve the actual lore chunks.
     * Note: We use rowid mapping here. If you need to map by lore_id, you can join with the actual table.
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
    suspend fun searchLoreChunks(queryEmbedding: ByteArray, topK: Int = 3): List<LoreChunkEntity>

    @SkipQueryVerification
    @Query("DELETE FROM vec_lore")
    suspend fun clearLoreVectors()

    @SkipQueryVerification
    @Query("DELETE FROM vec_memory")
    suspend fun clearMemoryVectors()
}
