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
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import org.eu.nl.syu.hearth.data.Character
import org.eu.nl.syu.hearth.data.ChatMessage
import org.eu.nl.syu.hearth.data.ChatThread
import org.eu.nl.syu.hearth.data.MessageRole

@Entity(tableName = "lore_chunks")
data class LoreChunkEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val text: String
)

@Entity(tableName = "memory_entries")
data class MemoryEntryEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val text: String,
    val timestamp: Long,
    val startMessageTimestamp: Long,
    val endMessageTimestamp: Long
)

@Entity(tableName = "chat_threads")
data class ChatThreadEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val title: String,
    val createdAt: Long,
    val lastMessageAt: Long,
    val sequenceId: Int,
    val styleJson: String? = null
)

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tagline: String,
    val avatarUrl: String?,
    val roleInstruction: String,
    val reminderMessage: String,
    val modelReference: String,
    val temp: Float,
    val topP: Float,
    val topK: Int,
    val enableThinking: Boolean,
    val enableThinkingCompatibility: Boolean,
    val thinkingCompatibilityToken: String,
    val includeThinkingInContext: Boolean,
    val sceneBackgroundUrl: String?,
    val isPredefined: Boolean,
    val lastUsedAt: Long
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatThreadEntity::class,
            parentColumns = ["id"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("threadId")]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val threadId: String? = null,
    val role: String,
    val content: String,
    val timestamp: Long,
    val isHiddenFromAi: Boolean,
    val modelReference: String? = null,
    val generationTimeMs: Long? = null,
    val tokensPerSecond: Float? = null,
    val parentId: String? = null,
    val versionGroupId: String? = null,
    val versionIndex: Int = 0
)

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY lastUsedAt DESC, name COLLATE NOCASE ASC")
    suspend fun getAllCharacters(): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterById(id: String): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity)

    @Query("UPDATE characters SET lastUsedAt = :lastUsedAt WHERE id = :id")
    suspend fun updateLastUsedAt(id: String, lastUsedAt: Long)

    @Query("UPDATE characters SET modelReference = :modelReference WHERE id = :id")
    suspend fun updateModelReference(id: String, modelReference: String)

    @Query(
        "UPDATE characters SET temp = :temp, topP = :topP, topK = :topK, enableThinking = :enableThinking, enableThinkingCompatibility = :enableThinkingCompatibility, thinkingCompatibilityToken = :thinkingCompatibilityToken, includeThinkingInContext = :includeThinkingInContext WHERE id = :id"
    )
    suspend fun updateSamplingSettings(
        id: String,
        temp: Float,
        topP: Float,
        topK: Int,
        enableThinking: Boolean,
        enableThinkingCompatibility: Boolean,
        thinkingCompatibilityToken: String,
        includeThinkingInContext: Boolean
    )

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)
}

@Dao
interface ChatThreadDao {
    @Query("SELECT * FROM chat_threads ORDER BY lastMessageAt DESC, createdAt DESC")
    fun getAllThreads(): Flow<List<ChatThreadEntity>>

    @Query("SELECT * FROM chat_threads WHERE id = :id")
    suspend fun getThreadById(id: String): ChatThreadEntity?

    @Query("SELECT * FROM chat_threads WHERE characterId = :characterId ORDER BY lastMessageAt DESC, createdAt DESC")
    suspend fun getThreadsForCharacter(characterId: String): List<ChatThreadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ChatThreadEntity)

    @Query("UPDATE chat_threads SET lastMessageAt = :lastMessageAt WHERE id = :id")
    suspend fun updateLastMessageAt(id: String, lastMessageAt: Long)

    @Query("SELECT COUNT(*) FROM chat_threads WHERE characterId = :characterId")
    suspend fun getThreadCountForCharacter(characterId: String): Int

    @Query("DELETE FROM chat_threads WHERE id = :id")
    suspend fun deleteThreadById(id: String)

    @Query("UPDATE chat_threads SET title = :title WHERE id = :id")
    suspend fun updateThreadTitle(id: String, title: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    suspend fun getMessagesForThread(threadId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE threadId = :threadId")
    suspend fun deleteMessagesForThread(threadId: String)

    @Query("UPDATE chat_messages SET isHiddenFromAi = 1 WHERE threadId = :threadId AND id IN (:messageIds)")
    suspend fun hideMessages(threadId: String, messageIds: List<String>)

    @Query("SELECT * FROM chat_messages WHERE versionGroupId = :versionGroupId ORDER BY versionIndex ASC")
    suspend fun getVersionsForGroup(versionGroupId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE id = :id")
    suspend fun getMessageById(id: String): ChatMessageEntity?

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM chat_messages WHERE threadId = :threadId AND timestamp >= :timestamp")
    suspend fun deleteMessagesAfter(threadId: String, timestamp: Long)

    @Query("UPDATE chat_messages SET content = :newContent WHERE id = :id")
    suspend fun updateMessageContent(id: String, newContent: String)

    @Query("UPDATE chat_threads SET title = :newTitle WHERE id = :id")
    suspend fun updateThreadTitle(id: String, newTitle: String)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_entries WHERE characterId = :characterId ORDER BY timestamp DESC")
    suspend fun getMemoriesForCharacter(characterId: String): List<MemoryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntryEntity)

    @Query("DELETE FROM memory_entries WHERE id = :id")
    suspend fun deleteMemory(id: String)

    @Query("SELECT * FROM memory_entries WHERE :messageTimestamp BETWEEN startMessageTimestamp AND endMessageTimestamp")
    suspend fun findMemoriesCoveringTimestamp(messageTimestamp: Long): List<MemoryEntryEntity>

    @Query("DELETE FROM memory_entries WHERE characterId = :characterId")
    suspend fun deleteMemoriesForCharacter(characterId: String)
}

@Dao
interface LoreChunkDao {
    @Query("SELECT * FROM lore_chunks WHERE characterId = :characterId")
    suspend fun getChunksForCharacter(characterId: String): List<LoreChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<LoreChunkEntity>)

    @Query("DELETE FROM lore_chunks WHERE characterId = :characterId")
    suspend fun deleteChunksForCharacter(characterId: String)
}

@Database(
    entities = [
        CharacterEntity::class, 
        ChatThreadEntity::class,
        ChatMessageEntity::class, 
        LoreChunkEntity::class, 
        MemoryEntryEntity::class
    ], 
    version = 11, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun chatThreadDao(): ChatThreadDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun loreChunkDao(): LoreChunkDao
    abstract fun vectorDao(): VectorDao
}

fun CharacterEntity.toDomain(): Character = Character(
    id = id,
    name = name,
    tagline = tagline,
    avatarUrl = avatarUrl,
    roleInstruction = roleInstruction,
    reminderMessage = reminderMessage,
    modelReference = modelReference,
    temp = temp,
    topP = topP,
    topK = topK,
    enableThinking = enableThinking,
    enableThinkingCompatibility = enableThinkingCompatibility,
    thinkingCompatibilityToken = thinkingCompatibilityToken,
    includeThinkingInContext = includeThinkingInContext,
    sceneBackgroundUrl = sceneBackgroundUrl,
    isPredefined = isPredefined,
    lastUsedAt = lastUsedAt
)

fun Character.toEntity(): CharacterEntity = CharacterEntity(
    id = id,
    name = name,
    tagline = tagline,
    avatarUrl = avatarUrl,
    roleInstruction = roleInstruction,
    reminderMessage = reminderMessage,
    modelReference = modelReference,
    temp = temp,
    topP = topP,
    topK = topK,
    enableThinking = enableThinking,
    enableThinkingCompatibility = enableThinkingCompatibility,
    thinkingCompatibilityToken = thinkingCompatibilityToken,
    includeThinkingInContext = includeThinkingInContext,
    sceneBackgroundUrl = sceneBackgroundUrl,
    isPredefined = isPredefined,
    lastUsedAt = lastUsedAt
)

fun ChatThreadEntity.toDomain(): ChatThread = ChatThread(
    id = id,
    characterId = characterId,
    title = title,
    createdAt = createdAt,
    lastMessageAt = lastMessageAt,
    sequenceId = sequenceId,
    styleJson = styleJson
)

fun ChatThread.toEntity(): ChatThreadEntity = ChatThreadEntity(
    id = id,
    characterId = characterId,
    title = title,
    createdAt = createdAt,
    lastMessageAt = lastMessageAt,
    sequenceId = sequenceId,
    styleJson = styleJson
)

fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    role = MessageRole.valueOf(role),
    content = content,
    timestamp = timestamp,
    isHiddenFromAi = isHiddenFromAi,
    modelReference = modelReference,
    generationTimeMs = generationTimeMs,
    tokensPerSecond = tokensPerSecond,
    parentId = parentId,
    versionGroupId = versionGroupId,
    versionIndex = versionIndex
)

fun ChatMessage.toEntity(characterId: String, threadId: String?): ChatMessageEntity = ChatMessageEntity(
    id = id,
    characterId = characterId,
    threadId = threadId,
    role = role.name,
    content = content,
    timestamp = timestamp,
    isHiddenFromAi = isHiddenFromAi,
    modelReference = modelReference,
    generationTimeMs = generationTimeMs,
    tokensPerSecond = tokensPerSecond,
    parentId = parentId,
    versionGroupId = versionGroupId,
    versionIndex = versionIndex
)
