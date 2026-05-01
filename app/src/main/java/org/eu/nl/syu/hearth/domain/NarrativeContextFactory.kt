/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.domain

import org.eu.nl.syu.hearth.data.Character
import org.eu.nl.syu.hearth.data.ChatMessage
import org.eu.nl.syu.hearth.data.MessageRole
import org.eu.nl.syu.hearth.data.local.ChatMessageDao
import org.eu.nl.syu.hearth.data.local.VectorDao
import org.eu.nl.syu.hearth.data.stripThinking
import org.eu.nl.syu.hearth.runtime.EmbeddingEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NarrativeContextFactory @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val vectorDao: VectorDao,
    private val chatMessageDao: ChatMessageDao
) {

    /**
     * Constructs the final prompt for the LLM.
     */
    suspend fun constructPrompt(
        userInput: String,
        character: Character,
        threadId: String?,
        history: List<ChatMessage>,
        userName: String = "User"
    ): String {
        val promptBuilder = StringBuilder()

        // 1. Identity: Templated roleInstruction
        val templatedRole = TemplateProcessor.process(character.roleInstruction, character, userName)
        promptBuilder.append(templatedRole).append("\n\n")

        // 2. Retrieved Lore (RAG): Top 5 matches across Global and Thread scopes
        val queryVector = embeddingEngine.getVector(userInput, isQuery = true)
        val loreChunks = vectorDao.searchLoreChunks(
            queryEmbedding = queryVector,
            characterId = character.id,
            threadId = threadId,
            topK = 5
        )
        if (loreChunks.isNotEmpty()) {
            promptBuilder.append("[Lore & Knowledge:]\n")
            loreChunks.forEach { promptBuilder.append("- ").append(it.text).append("\n") }
            promptBuilder.append("\n")
        }

        // 3. Retrieved Memories (RAG): Top 2-3
        val memories = vectorDao.searchMemoryEntries(queryVector, topK = 3)
        if (memories.isNotEmpty()) {
            promptBuilder.append("[Past Memories:]\n")
            memories.forEach { promptBuilder.append("- ").append(it.text).append("\n") }
            promptBuilder.append("\n")
        }

        // 4. Conversation History
        if (history.isNotEmpty()) {
            promptBuilder.append("[Recent Conversation:]\n")
            history.filter { !it.isHiddenFromAi }.forEach { msg ->
                val roleName = if (msg.role == MessageRole.USER) userName else character.name
                val content = if (character.includeThinkingInContext) {
                    msg.content
                } else {
                    msg.content.stripThinking()
                }
                if (content.isNotBlank()) {
                    promptBuilder.append("$roleName: $content\n")
                }
            }
        }

        // 5. The Reminder: Templated reminderMessage appended at the end
        if (character.reminderMessage.isNotBlank()) {
            val templatedReminder = TemplateProcessor.process(character.reminderMessage, character, userName)
            promptBuilder.append("\n[Reminder: ").append(templatedReminder).append("]")
        }

        // Assistant trigger
        promptBuilder.append("\n").append(character.name).append(": ")

        return promptBuilder.toString()
    }
}
