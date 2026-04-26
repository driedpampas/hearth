package org.eu.nl.syu.charchat.di

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
import org.eu.nl.syu.charchat.data.DefaultCharacters
import org.eu.nl.syu.charchat.data.local.AppDatabase
import org.eu.nl.syu.charchat.data.local.CharacterDao
import org.eu.nl.syu.charchat.data.local.ChatMessageDao
import org.eu.nl.syu.charchat.data.local.VectorDao
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
            .addMigrations(MIGRATION_2_3)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    initializeDatabase(db, sqliteVecPath)
                }

                override fun onOpen(connection: SQLiteConnection) {
                    super.onOpen(connection)
                    initializeDatabase(connection, sqliteVecPath)
                }
            })
            .build()
    }

    private fun initializeDatabase(db: SupportSQLiteDatabase, sqliteVecPath: String) {
        // Create virtual tables
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

    private fun initializeDatabase(connection: SQLiteConnection, sqliteVecPath: String) {
        // Create virtual tables
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
                    modelReference, temp, topP, topK, sceneBackgroundUrl, isPredefined, lastUsedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                    modelReference, temp, topP, topK, sceneBackgroundUrl, isPredefined, lastUsedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            if (character.sceneBackgroundUrl == null) statement.bindNull(11) else statement.bindText(11, character.sceneBackgroundUrl)
            statement.bindBoolean(12, character.isPredefined)
            statement.bindLong(13, character.lastUsedAt)
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
