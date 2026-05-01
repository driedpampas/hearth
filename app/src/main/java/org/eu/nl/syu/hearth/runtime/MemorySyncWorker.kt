/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.runtime

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.eu.nl.syu.hearth.data.MessageRole
import org.eu.nl.syu.hearth.data.local.AppDatabase
import org.eu.nl.syu.hearth.data.local.MemoryEntryEntity
import org.eu.nl.syu.hearth.data.local.toDomain
import java.util.UUID

@HiltWorker
class MemorySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val db: AppDatabase,
    private val engineWrapper: LiteRtEngineWrapper,
    private val embeddingEngine: EmbeddingEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val characterId = inputData.getString("characterId") ?: return Result.failure()
        val threadId = inputData.getString("threadId") ?: return Result.failure()
        val startTs = inputData.getLong("startTs", 0L)
        val endTs = inputData.getLong("endTs", 0L)
        val modelPath = inputData.getString("modelPath") ?: return Result.failure()

        try {
            // 1. Ensure engine is initialized for summarization
            if (!engineWrapper.isInitialized() || engineWrapper.getLoadedModelPath() != modelPath) {
                engineWrapper.initialize(modelPath)
            }

            // 2. Fetch messages in range
            val allMessages = db.chatMessageDao().getMessagesForThread(threadId)
                .map { it.toDomain() }
                .filter { it.timestamp in startTs..endTs && !it.isHiddenFromAi }

            if (allMessages.isEmpty()) return Result.success()

            // 3. Summarize
            val contextText = allMessages.joinToString("\n") { msg ->
                val roleName = if (msg.role == MessageRole.USER) "User" else "Assistant"
                "$roleName: ${msg.content}"
            }

            val prompt = """
                Summarize the following conversation snippet for long-term memory. 
                Focus on key facts, events, and character developments. 
                Be concise but thorough.
                
                Conversation:
                $contextText
                
                Summary:
            """.trimIndent()

            var summary = ""
            engineWrapper.sendMessage(prompt).collect { partial ->
                summary += partial
            }

            if (summary.isBlank()) return Result.retry()

            // 4. Embed summary
            val vector = embeddingEngine.getVector(summary, isQuery = false)

            // 5. Store new memory
            val memoryId = UUID.randomUUID().toString()
            val memoryEntry = MemoryEntryEntity(
                id = memoryId,
                characterId = characterId,
                text = summary,
                timestamp = System.currentTimeMillis(),
                startMessageTimestamp = startTs,
                endMessageTimestamp = endTs
            )

            db.memoryDao().insertMemory(memoryEntry)
            db.vectorDao().insertMemoryEmbedding(memoryId, vector)

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
