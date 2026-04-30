package org.eu.nl.syu.charchat.data.local

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
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.data.ChatMessage
import org.eu.nl.syu.charchat.data.ChatThread
import org.eu.nl.syu.charchat.data.MessageRole
import kotlinx.coroutines.flow.Flow

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
    val timestamp: Long
)

@Entity(tableName = "chat_threads")
data class ChatThreadEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val title: String,
    val createdAt: Long,
    val lastMessageAt: Long,
    val sequenceId: Int
)

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tagline: String,
    val avatarUrl: String?,
    val systemPromptLore: String,
    val reminderMessage: String,
    val modelReference: String,
    val temp: Float,
    val topP: Float,
    val topK: Int,
    val enableThinking: Boolean,
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
    val tokensPerSecond: Float? = null
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
        "UPDATE characters SET temp = :temp, topP = :topP, topK = :topK, enableThinking = :enableThinking WHERE id = :id"
    )
    suspend fun updateSamplingSettings(id: String, temp: Float, topP: Float, topK: Int, enableThinking: Boolean)

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
}

@Database(
    entities = [
        CharacterEntity::class, 
        ChatThreadEntity::class,
        ChatMessageEntity::class, 
        LoreChunkEntity::class, 
        MemoryEntryEntity::class
    ], 
    version = 7, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun chatThreadDao(): ChatThreadDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun vectorDao(): VectorDao
}

fun CharacterEntity.toDomain(): Character = Character(
    id = id,
    name = name,
    tagline = tagline,
    avatarUrl = avatarUrl,
    systemPromptLore = systemPromptLore,
    reminderMessage = reminderMessage,
    modelReference = modelReference,
    temp = temp,
    topP = topP,
    topK = topK,
    enableThinking = enableThinking,
    sceneBackgroundUrl = sceneBackgroundUrl,
    isPredefined = isPredefined,
    lastUsedAt = lastUsedAt
)

fun Character.toEntity(): CharacterEntity = CharacterEntity(
    id = id,
    name = name,
    tagline = tagline,
    avatarUrl = avatarUrl,
    systemPromptLore = systemPromptLore,
    reminderMessage = reminderMessage,
    modelReference = modelReference,
    temp = temp,
    topP = topP,
    topK = topK,
    enableThinking = enableThinking,
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
    sequenceId = sequenceId
)

fun ChatThread.toEntity(): ChatThreadEntity = ChatThreadEntity(
    id = id,
    characterId = characterId,
    title = title,
    createdAt = createdAt,
    lastMessageAt = lastMessageAt,
    sequenceId = sequenceId
)

fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    role = MessageRole.valueOf(role),
    content = content,
    timestamp = timestamp,
    isHiddenFromAi = isHiddenFromAi,
    modelReference = modelReference,
    generationTimeMs = generationTimeMs,
    tokensPerSecond = tokensPerSecond
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
    tokensPerSecond = tokensPerSecond
)
