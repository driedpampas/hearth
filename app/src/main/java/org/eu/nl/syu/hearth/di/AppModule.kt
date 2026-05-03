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
import org.eu.nl.syu.hearth.data.local.MemoryDao
import org.eu.nl.syu.hearth.data.local.LoreChunkDao
import org.eu.nl.syu.hearth.data.local.VectorDao
import org.eu.nl.syu.hearth.data.local.UserPersonaDao
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
            "hearth_db"
        )
            .setDriver(driver)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration(true)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    seedDefaultAssistant(db)
                }

                override fun onCreate(connection: SQLiteConnection) {
                    super.onCreate(connection)
                    seedDefaultAssistant(connection)
                }

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
    }

    private fun seedDefaultAssistant(db: SupportSQLiteDatabase) {
        db.query("SELECT COUNT(*) FROM characters WHERE id='${DefaultCharacters.ASSISTANT_CHARACTER_ID}'").use { cursor ->
            if (cursor.moveToFirst() && cursor.getLong(0) > 0L) return
        }

        val character = DefaultCharacters.assistantCharacter()
        db.execSQL(
            """
                INSERT INTO characters (
                    id, name, tagline, avatarUrl, roleInstruction, reminderMessage,
                    modelReference, temp, topP, topK, enableThinking, enableThinkingCompatibility, thinkingCompatibilityToken, includeThinkingInContext, knowledgeBase, sceneBackgroundUrl, isPredefined, initialMessagesJson, lastUsedAt, defaultUserPersonaId
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                character.id,
                character.name,
                character.tagline,
                character.avatarUrl,
                character.roleInstruction,
                character.reminderMessage,
                character.modelReference,
                character.temp,
                character.topP,
                character.topK,
                character.enableThinking,
                character.enableThinkingCompatibility,
                character.thinkingCompatibilityToken,
                character.includeThinkingInContext,
                character.knowledgeBase,
                character.sceneBackgroundUrl,
                if (character.isPredefined) 1 else 0, "[]",
                character.lastUsedAt,
                null
            )
        )
    }

    private fun seedDefaultAssistant(connection: SQLiteConnection) {
        if (queryExists(connection, DefaultCharacters.ASSISTANT_CHARACTER_ID)) return

        val character = DefaultCharacters.assistantCharacter()
        connection.prepare(
            """
                INSERT INTO characters (
                    id, name, tagline, avatarUrl, roleInstruction, reminderMessage,
                    modelReference, temp, topP, topK, enableThinking, enableThinkingCompatibility, thinkingCompatibilityToken, includeThinkingInContext, knowledgeBase, sceneBackgroundUrl, isPredefined, initialMessagesJson, lastUsedAt, defaultUserPersonaId
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.bindText(1, character.id)
            statement.bindText(2, character.name)
            statement.bindText(3, character.tagline)
            if (character.avatarUrl == null) statement.bindNull(4) else statement.bindText(4, character.avatarUrl)
            statement.bindText(5, character.roleInstruction)
            statement.bindText(6, character.reminderMessage)
            statement.bindText(7, character.modelReference)
            statement.bindDouble(8, character.temp.toDouble())
            statement.bindDouble(9, character.topP.toDouble())
            statement.bindLong(10, character.topK.toLong())
            statement.bindBoolean(11, character.enableThinking)
            statement.bindBoolean(12, character.enableThinkingCompatibility)
            statement.bindText(13, character.thinkingCompatibilityToken)
            statement.bindBoolean(14, character.includeThinkingInContext)
            statement.bindText(15, character.knowledgeBase)
            if (character.sceneBackgroundUrl == null) statement.bindNull(16) else statement.bindText(16, character.sceneBackgroundUrl)
            statement.bindBoolean(17, character.isPredefined)
            statement.bindText(18, "[]")
            statement.bindLong(19, character.lastUsedAt)
            statement.bindNull(20)
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

    @Provides
    fun provideMemoryDao(database: AppDatabase): MemoryDao {
        return database.memoryDao()
    }

    @Provides
    fun provideLoreChunkDao(database: AppDatabase): LoreChunkDao {
        return database.loreChunkDao()
    }

    @Provides
    fun provideUserPersonaDao(database: AppDatabase): UserPersonaDao {
        return database.userPersonaDao()
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            execute(connection, "ALTER TABLE chat_messages ADD COLUMN isError INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            // Update characters table
            execute(connection, "ALTER TABLE characters ADD COLUMN defaultUserPersonaId TEXT")
            
            // Update chat_threads table
            execute(connection, "ALTER TABLE chat_threads ADD COLUMN userPersonaId TEXT")
            execute(connection, "ALTER TABLE chat_threads ADD COLUMN threadUserPersonaBio TEXT")
            execute(connection, "ALTER TABLE chat_threads ADD COLUMN threadCharacterOverrideJson TEXT")
            
            // Create user_personas table
            execute(connection, """
                CREATE TABLE IF NOT EXISTS user_personas (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    bio TEXT NOT NULL,
                    avatarUrl TEXT,
                    lastUsedAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }
}
