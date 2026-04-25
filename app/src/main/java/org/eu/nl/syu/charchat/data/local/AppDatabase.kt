package org.eu.nl.syu.charchat.data.local

import androidx.room.*
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.data.ChatMessage
import org.eu.nl.syu.charchat.data.MessageRole

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
    val sceneBackgroundUrl: String?,
    val isPredefined: Boolean
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterId")]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val isHiddenFromAi: Boolean
)

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters")
    suspend fun getAllCharacters(): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterById(id: String): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity)

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE characterId = :characterId ORDER BY timestamp ASC")
    suspend fun getMessagesForCharacter(characterId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE characterId = :characterId")
    suspend fun deleteMessagesForCharacter(characterId: String)
}

@Database(entities = [CharacterEntity::class, ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun chatMessageDao(): ChatMessageDao
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
    sceneBackgroundUrl = sceneBackgroundUrl,
    isPredefined = isPredefined
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
    sceneBackgroundUrl = sceneBackgroundUrl,
    isPredefined = isPredefined
)

fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    role = MessageRole.valueOf(role),
    content = content,
    timestamp = timestamp,
    isHiddenFromAi = isHiddenFromAi
)

fun ChatMessage.toEntity(characterId: String): ChatMessageEntity = ChatMessageEntity(
    id = id,
    characterId = characterId,
    role = role.name,
    content = content,
    timestamp = timestamp,
    isHiddenFromAi = isHiddenFromAi
)
