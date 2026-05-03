/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 */

package org.eu.nl.syu.hearth.domain

import org.eu.nl.syu.hearth.data.Character
import org.eu.nl.syu.hearth.data.ChatMessage
import org.eu.nl.syu.hearth.data.MessageRole
import org.eu.nl.syu.hearth.data.stripThinking
import org.eu.nl.syu.hearth.data.local.ChatMessageDao
import org.eu.nl.syu.hearth.data.local.ChatThreadDao
import org.eu.nl.syu.hearth.data.local.UserPersonaDao
import org.eu.nl.syu.hearth.data.local.VectorDao
import org.eu.nl.syu.hearth.data.ModelRepository
import org.eu.nl.syu.hearth.runtime.EmbeddingEngine
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NarrativeContextFactory @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val vectorDao: VectorDao,
    private val chatMessageDao: ChatMessageDao,
    private val chatThreadDao: ChatThreadDao,
    private val userPersonaDao: UserPersonaDao,
    private val modelRepository: ModelRepository
) {
    private val gson = Gson()

    data class CharacterOverride(
        val roleInstruction: String? = null,
        val reminderMessage: String? = null,
        val temp: Float? = null,
        val topP: Float? = null,
        val topK: Int? = null,
        val enableThinking: Boolean? = null,
        val enableThinkingCompatibility: Boolean? = null,
        val thinkingCompatibilityToken: String? = null,
        val includeThinkingInContext: Boolean? = null
    )

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
        val thread = threadId?.let { chatThreadDao.getThreadById(it) }
        
        // 0. Resolve Character Overrides
        val overrides = thread?.threadCharacterOverrideJson?.let {
            try { gson.fromJson(it, CharacterOverride::class.java) } catch (e: Exception) { null }
        }
        
        val effectiveRoleInstruction = overrides?.roleInstruction ?: character.roleInstruction
        val effectiveReminderMessage = overrides?.reminderMessage ?: character.reminderMessage
        val effectiveIncludeThinking = overrides?.includeThinkingInContext ?: character.includeThinkingInContext
        
        // 1. Resolve User Identity
        val userBio = when {
            thread?.threadUserPersonaBio != null -> thread.threadUserPersonaBio
            thread?.userPersonaId != null -> userPersonaDao.getPersonaById(thread.userPersonaId)?.bio
            character.defaultUserPersonaId != null -> userPersonaDao.getPersonaById(character.defaultUserPersonaId)?.bio
            else -> modelRepository.globalDefaultPersonaId.first()?.let { userPersonaDao.getPersonaById(it)?.bio }
        }
        
        val personaName = when {
            thread?.userPersonaId != null -> userPersonaDao.getPersonaById(thread.userPersonaId)?.name
            character.defaultUserPersonaId != null -> userPersonaDao.getPersonaById(character.defaultUserPersonaId)?.name
            else -> modelRepository.globalDefaultPersonaId.first()?.let { userPersonaDao.getPersonaById(it)?.name }
        } ?: userName

        val promptBuilder = StringBuilder()

        // 2. Identity: Templated roleInstruction
        val templatedRole = TemplateProcessor.process(effectiveRoleInstruction, character, personaName, userBio)
        promptBuilder.append(templatedRole).append("\n\n")

        // 3. Retrieved Lore (RAG): Top 5 matches across Global and Thread scopes
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

        // 4. Retrieved Memories (RAG): Top 2-3
        val memories = vectorDao.searchMemoryEntries(queryVector, topK = 3)
        if (memories.isNotEmpty()) {
            promptBuilder.append("[Past Memories:]\n")
            memories.forEach { promptBuilder.append("- ").append(it.text).append("\n") }
            promptBuilder.append("\n")
        }

        // 5. Conversation History
        if (history.isNotEmpty()) {
            promptBuilder.append("[Recent Conversation:]\n")
            history.filter { !it.isHiddenFromAi }.forEach { msg ->
                val roleName = if (msg.role == MessageRole.USER) personaName else character.name
                val content = if (effectiveIncludeThinking) {
                    msg.content
                } else {
                    msg.content.stripThinking()
                }
                if (content.isNotBlank()) {
                    promptBuilder.append("$roleName: $content\n")
                }
            }
        }

        // 6. The Reminder: Templated reminderMessage appended at the end
        if (effectiveReminderMessage.isNotBlank()) {
            val templatedReminder = TemplateProcessor.process(effectiveReminderMessage, character, personaName, userBio)
            promptBuilder.append("\n[Reminder: ").append(templatedReminder).append("]")
        }

        // Assistant trigger
        promptBuilder.append("\n").append(character.name).append(": ")

        return promptBuilder.toString()
    }
}
