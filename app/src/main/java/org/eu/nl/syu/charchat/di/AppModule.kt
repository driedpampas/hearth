package org.eu.nl.syu.charchat.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "charchat_db"
        )
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_2_3)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Load the sqlite-vec extension
                    db.query("SELECT load_extension('$sqliteVecPath')").close()

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
            })
            .build()
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
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE characters ADD COLUMN lastUsedAt INTEGER NOT NULL DEFAULT 0")
    }
}
