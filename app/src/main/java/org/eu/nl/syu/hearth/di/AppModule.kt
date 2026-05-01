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

package org.eu.nl.syu.hearth.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.eu.nl.syu.hearth.data.DefaultCharacters
import org.eu.nl.syu.hearth.data.local.AppDatabase
import org.eu.nl.syu.hearth.data.local.CharacterDao
import org.eu.nl.syu.hearth.data.local.ChatMessageDao
import org.eu.nl.syu.hearth.data.local.ChatThreadDao
import org.eu.nl.syu.hearth.data.local.VectorDao
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        val nativeLibraryDir = context.applicationInfo.nativeLibraryDir
        val sqliteVecPath = File(nativeLibraryDir, "libsqlite_vec.so").absolutePath
        val driver = BundledSQLiteDriver().apply {
            addExtension(sqliteVecPath, "sqlite3_vec_init")
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "charchat_db"
        )
            .setDriver(driver)
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    initializeDatabase(db)
                }

                override fun onOpen(connection: SQLiteConnection) {
                    super.onOpen(connection)
                    initializeDatabase(connection)
                }
            })
            .build()
    }

    private fun initializeDatabase(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                CREATE VIRTUAL TABLE IF NOT EXISTS vec_lore USING vec0(
                    lore_id TEXT PRIMARY KEY,
                    embedding float[256]
                )
            """.trimIndent()
        )
        db.execSQL(
            """
                CREATE VIRTUAL TABLE IF NOT EXISTS vec_memory USING vec0(
                    memory_id TEXT PRIMARY KEY,
                    embedding float[256]
                )
            """.trimIndent()
        )
        seedDefaultAssistant(db)
    }

    private fun initializeDatabase(connection: SQLiteConnection) {
        execute(
            connection,
            """
                CREATE VIRTUAL TABLE IF NOT EXISTS vec_lore USING vec0(
                    lore_id TEXT PRIMARY KEY,
                    embedding float[256]
                )
            """.trimIndent()
        )
        execute(
            connection,
            """
                CREATE VIRTUAL TABLE IF NOT EXISTS vec_memory USING vec0(
                    memory_id TEXT PRIMARY KEY,
                    embedding float[256]
                )
            """.trimIndent()
        )
        seedDefaultAssistant(connection)
    }

    private fun seedDefaultAssistant(db: SupportSQLiteDatabase) {
        db.query("SELECT COUNT(*) FROM characters WHERE id='${DefaultCharacters.ASSISTANT_CHARACTER_ID}'").use { cursor ->
            if (cursor.moveToFirst() && cursor.getLong(0) > 0L) return
        }

        val character = DefaultCharacters.assistantCharacter()
        db.execSQL(
            """
                INSERT INTO characters (
                    id, name, tagline, avatarUrl, systemPromptLore, reminderMessage,
                    modelReference, temp, topP, topK, enableThinking, enableThinkingCompatibility, thinkingCompatibilityToken, includeThinkingInContext, sceneBackgroundUrl, isPredefined, lastUsedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                character.id,
                character.name,
                character.tagline,
                character.avatarUrl,
                character.systemPromptLore,
                character.reminderMessage,
                character.modelReference,
                character.temp,
                character.topP,
                character.topK,
                character.enableThinking,
                character.enableThinkingCompatibility,
                character.thinkingCompatibilityToken,
                character.includeThinkingInContext,
                character.sceneBackgroundUrl,
                if (character.isPredefined) 1 else 0,
                character.lastUsedAt
            )
        )
    }

    private fun seedDefaultAssistant(connection: SQLiteConnection) {
        if (queryExists(connection, DefaultCharacters.ASSISTANT_CHARACTER_ID)) return

        val character = DefaultCharacters.assistantCharacter()
        connection.prepare(
            """
                INSERT INTO characters (
                    id, name, tagline, avatarUrl, systemPromptLore, reminderMessage,
                    modelReference, temp, topP, topK, enableThinking, enableThinkingCompatibility, thinkingCompatibilityToken, includeThinkingInContext, sceneBackgroundUrl, isPredefined, lastUsedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.bindText(1, character.id)
            statement.bindText(2, character.name)
            statement.bindText(3, character.tagline)
            if (character.avatarUrl == null) statement.bindNull(4) else statement.bindText(4, character.avatarUrl)
            statement.bindText(5, character.systemPromptLore)
            statement.bindText(6, character.reminderMessage)
            statement.bindText(7, character.modelReference)
            statement.bindDouble(8, character.temp.toDouble())
            statement.bindDouble(9, character.topP.toDouble())
            statement.bindLong(10, character.topK.toLong())
            statement.bindBoolean(11, character.enableThinking)
            statement.bindBoolean(12, character.enableThinkingCompatibility)
            statement.bindText(13, character.thinkingCompatibilityToken)
            statement.bindBoolean(14, character.includeThinkingInContext)
            if (character.sceneBackgroundUrl == null) statement.bindNull(15) else statement.bindText(15, character.sceneBackgroundUrl)
            statement.bindBoolean(16, character.isPredefined)
            statement.bindLong(17, character.lastUsedAt)
            statement.step()
        }
    }

    private fun execute(connection: SQLiteConnection, sql: String) {
        connection.prepare(sql).use { statement ->
            statement.step()
        }
    }

    private fun queryExists(connection: SQLiteConnection, characterId: String): Boolean {
        connection.prepare("SELECT EXISTS(SELECT 1 FROM characters WHERE id = ?1)").use { statement ->
            statement.bindText(1, characterId)
            return statement.step() && statement.getLong(0) > 0L
        }
    }

    @Provides
    fun provideCharacterDao(database: AppDatabase): CharacterDao {
        return database.characterDao()
    }

    @Provides
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    fun provideChatThreadDao(database: AppDatabase): ChatThreadDao {
        return database.chatThreadDao()
    }

    @Provides
    fun provideVectorDao(database: AppDatabase): VectorDao {
        return database.vectorDao()
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE characters ADD COLUMN lastUsedAt INTEGER NOT NULL DEFAULT 0").use { statement ->
            statement.step()
        }
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE chat_messages ADD COLUMN modelReference TEXT").use { statement ->
            statement.step()
        }
        connection.prepare("ALTER TABLE chat_messages ADD COLUMN generationTimeMs INTEGER").use { statement ->
            statement.step()
        }
        connection.prepare("ALTER TABLE chat_messages ADD COLUMN tokensPerSecond REAL").use { statement ->
            statement.step()
        }
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("CREATE TABLE IF NOT EXISTS chat_threads (id TEXT NOT NULL PRIMARY KEY, characterId TEXT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL, lastMessageAt INTEGER NOT NULL)").use { it.step() }
        connection.prepare("CREATE TABLE IF NOT EXISTS chat_messages_new (id TEXT NOT NULL PRIMARY KEY, characterId TEXT NOT NULL, threadId TEXT, role TEXT NOT NULL, content TEXT NOT NULL, timestamp INTEGER NOT NULL, isHiddenFromAi INTEGER NOT NULL, modelReference TEXT, generationTimeMs INTEGER, tokensPerSecond REAL, FOREIGN KEY(threadId) REFERENCES chat_threads(id) ON DELETE CASCADE)").use { it.step() }
        connection.prepare("INSERT INTO chat_messages_new (id, characterId, threadId, role, content, timestamp, isHiddenFromAi, modelReference, generationTimeMs, tokensPerSecond) SELECT id, characterId, NULL, role, content, timestamp, isHiddenFromAi, modelReference, generationTimeMs, tokensPerSecond FROM chat_messages").use { it.step() }
        connection.prepare("DROP TABLE chat_messages").use { it.step() }
        connection.prepare("ALTER TABLE chat_messages_new RENAME TO chat_messages").use { it.step() }
        connection.prepare("CREATE INDEX IF NOT EXISTS index_chat_messages_threadId ON chat_messages(threadId)").use { it.step() }
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE characters ADD COLUMN enableThinking INTEGER NOT NULL DEFAULT 0").use { it.step() }
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE chat_threads ADD COLUMN sequenceId INTEGER NOT NULL DEFAULT 0").use { it.step() }
        connection.prepare("""
            UPDATE chat_threads 
            SET sequenceId = (
                SELECT COUNT(*) 
                FROM chat_threads AS t2 
                WHERE t2.characterId = chat_threads.characterId 
                AND (t2.createdAt < chat_threads.createdAt OR (t2.createdAt = chat_threads.createdAt AND t2.id <= chat_threads.id))
            )
        """.trimIndent()).use { it.step() }
    }
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE chat_messages ADD COLUMN parentId TEXT").use { it.step() }
        connection.prepare("ALTER TABLE chat_messages ADD COLUMN versionGroupId TEXT").use { it.step() }
        connection.prepare("ALTER TABLE chat_messages ADD COLUMN versionIndex INTEGER NOT NULL DEFAULT 0").use { it.step() }
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE characters ADD COLUMN enableThinkingCompatibility INTEGER NOT NULL DEFAULT 0").use { it.step() }
        connection.prepare("ALTER TABLE characters ADD COLUMN thinkingCompatibilityToken TEXT NOT NULL DEFAULT ''").use { it.step() }
    }
}

private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE characters ADD COLUMN includeThinkingInContext INTEGER NOT NULL DEFAULT 0").use { it.step() }
    }
}
