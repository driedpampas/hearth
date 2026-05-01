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

package org.eu.nl.syu.hearth.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eu.nl.syu.hearth.data.Character
import org.eu.nl.syu.hearth.data.ChatMessage
import org.eu.nl.syu.hearth.data.ChatThread
import org.eu.nl.syu.hearth.data.MessageRole
import org.eu.nl.syu.hearth.data.ModelManager
import org.eu.nl.syu.hearth.data.ModelRepository
import org.eu.nl.syu.hearth.data.local.CharacterDao
import org.eu.nl.syu.hearth.data.local.ChatMessageDao
import org.eu.nl.syu.hearth.data.local.ChatThreadDao
import org.eu.nl.syu.hearth.data.local.toDomain
import org.eu.nl.syu.hearth.data.local.toEntity
import org.eu.nl.syu.hearth.runtime.EmbeddingEngine
import org.eu.nl.syu.hearth.runtime.LiteRtEngineWrapper
import java.io.File
import javax.inject.Inject

enum class DeletionMode {
    ONLY_THIS, EVERYTHING_AFTER
}

data class ChatUiState(
    val character: Character? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val currentGeneratingText: String = "",
    val tokenCount: Int = 0,
    val maxTokens: Int = 4096,
    val modelError: String? = null,
    val isRawModel: Boolean = false,
    val fallbackReason: String? = null,
    val isLoadingModel: Boolean = false,
    val threadTitle: String? = null,
    val versionCounts: Map<String, Int> = emptyMap(),
    val displayedVersions: Map<String, Int> = emptyMap(),
    val regeneratingMessageId: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val characterDao: CharacterDao,
    private val chatThreadDao: ChatThreadDao,
    private val chatMessageDao: ChatMessageDao,
    private val engineWrapper: LiteRtEngineWrapper,
    private val embeddingEngine: EmbeddingEngine,
    private val modelManager: ModelManager,
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var activeThreadId: String? = null
    private var isThreadSaved: Boolean = false
    private var generationJob: kotlinx.coroutines.Job? = null

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        _uiState.update { it.copy(isGenerating = false, currentGeneratingText = "", regeneratingMessageId = null) }
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            val threadEntity = chatThreadDao.getThreadById(conversationId)
            isThreadSaved = threadEntity != null
            val character = when {
                threadEntity != null -> characterDao.getCharacterById(threadEntity.characterId)?.toDomain()
                else -> characterDao.getCharacterById(conversationId)?.toDomain()
            }
            
            if (character == null) {
                return@launch
            }

            // If we don't have a thread entity for this ID, check if it's a characterId and if they have an existing thread
            val thread = when {
                threadEntity != null -> threadEntity.toDomain()
                else -> ChatThread(
                    characterId = character.id,
                    title = character.name
                )
            }
            
            val threadId = thread.id
            activeThreadId = threadId
            isThreadSaved = chatThreadDao.getThreadById(threadId) != null

            val openedAt = System.currentTimeMillis()
            characterDao.updateLastUsedAt(character.id, openedAt)
            
            val allMessages = chatMessageDao.getMessagesForThread(threadId).map { it.toDomain() }
            
            // Group by versionGroupId. If versionGroupId is null, treat as single version.
            val grouped = allMessages.groupBy { it.versionGroupId ?: it.id }
            val versionCounts = grouped.mapValues { it.value.size }
            
            // For each group, pick the highest index by default
            val displayedVersions = grouped.mapValues { it.value.maxByOrNull { m -> m.versionIndex }?.versionIndex ?: 0 }
            
            // Filter messages to only show the ones matching displayedVersions
            val visibleMessages = grouped.mapNotNull { (vgId, versions) ->
                val activeIndex = displayedVersions[vgId] ?: 0
                versions.find { it.versionIndex == activeIndex }
            }.sortedBy { it.timestamp }

            val initialMaxTokens = modelRepository.defaultMaxTokens.first()
            
            _uiState.update {
                it.copy(
                    character = character.copy(lastUsedAt = openedAt),
                    messages = visibleMessages,
                    modelError = null,
                    isRawModel = false,
                    fallbackReason = null,
                    isLoadingModel = false,
                    tokenCount = visibleMessages.sumOf { message -> (message.content.length / 4).coerceAtLeast(0) },
                    threadTitle = thread.title,
                    maxTokens = initialMaxTokens,
                    versionCounts = versionCounts,
                    displayedVersions = displayedVersions
                )
            }

            // Path resolution
            val modelPath = resolveModelPath(character.modelReference)
            val autoLoadEnabled = modelRepository.autoLoadChatModel.first()

            // If no model is loaded and auto-load is off, just show history (which we already did)
            if (!engineWrapper.isInitialized() && !autoLoadEnabled) {
                _uiState.update {
                    it.copy(
                        modelError = when {
                            character.modelReference.isBlank() -> "Select a model first."
                            modelPath == null -> "Model file not found: ${character.modelReference}.\nDid you download it?"
                            else -> "Model not loaded."
                        }
                    )
                }
                return@launch
            }

            // If model path is missing but auto-load is on, we still can't load it
            if (modelPath == null && !engineWrapper.isInitialized()) {
                _uiState.update {
                    it.copy(
                        modelError = if (character.modelReference.isBlank()) {
                            "Select a model first."
                        } else {
                            "Model file not found: ${character.modelReference}.\nDid you download it?"
                        }
                    )
                }
                return@launch
            }
            



            // Now handle model loading
            if (!engineWrapper.isInitialized() && autoLoadEnabled) {
                if (modelPath == null) {
                    _uiState.update { it.copy(modelError = "Select a model first.") }
                } else {
                    _uiState.update { it.copy(isLoadingModel = true) }
                    val modelFile = File(modelPath)
                    try {
                        withContext(Dispatchers.IO) {
                            engineWrapper.initialize(modelPath, maxTokens = initialMaxTokens)
                        }
                        _uiState.update { it.copy(isLoadingModel = false) }
                    } catch (e: Exception) {
                        val msg = when {
                            isInputTensorMissing(e) -> {
                                modelRepository.quarantineModel(modelFile)
                                "This model is corrupted/incompatible and has been removed. Please select a compatible LiteRT LM chat model."
                            }
                            else -> {
                                "Failed to initialize the model: ${e.message}"
                            }
                        }
                        _uiState.update { it.copy(modelError = msg, isLoadingModel = false) }
                    }
                }
            } else if (!engineWrapper.isInitialized()) {
                _uiState.update { it.copy(modelError = "Model not loaded.") }
            }

            // Sync raw model state to UI
            viewModelScope.launch {
                engineWrapper.isRawModel.collect { isRaw ->
                    _uiState.update { it.copy(isRawModel = isRaw) }
                }
            }
            viewModelScope.launch {
                engineWrapper.fallbackReason.collect { reason ->
                    _uiState.update { it.copy(fallbackReason = reason) }
                }
            }

            if (engineWrapper.isInitialized() && !engineWrapper.isRawModel.value) {
                engineWrapper.createConversation(character)
            }
        }
    }

    // Removed immediate createNewThread insertion to avoid empty threads in history.

    private fun isUnsupportedChatModel(modelReference: String): Boolean {
        return false // Allow everything now
    }

    private fun isInputTensorMissing(e: Exception): Boolean {
        val msg = e.message ?: return false
        return msg.contains("Input tensor not found", ignoreCase = true) ||
                msg.contains("NOT_FOUND", ignoreCase = true) ||
                msg.contains("tensor not found", ignoreCase = true)
    }

    private fun resolveModelPath(modelReference: String): String? {
        if (modelReference.isBlank()) return null

        val explicit = File(modelReference)
        if (explicit.exists() && explicit.isFile) return explicit.absolutePath

        val modelsDir = modelManager.getModelsDir()
        val localFile = File(modelsDir, modelReference)
        if (localFile.exists() && localFile.isFile) return localFile.absolutePath

        val baseName = modelReference.substringBeforeLast('.')
        val inferred = modelManager.getLocalModels().firstOrNull { file ->
            file.isFile && (file.name == modelReference || file.nameWithoutExtension == baseName)
        }
        return inferred?.absolutePath
    }

    fun loadModel() {
        val character = _uiState.value.character ?: return
        val modelPath = resolveModelPath(character.modelReference) ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModel = true, modelError = null) }
            try {
                val maxTokens = modelRepository.defaultMaxTokens.first()
                withContext(Dispatchers.IO) {
                    engineWrapper.initialize(modelPath, maxTokens = maxTokens)
                }
                _uiState.update { it.copy(isLoadingModel = false, maxTokens = maxTokens) }
                engineWrapper.createConversation(character)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingModel = false, modelError = "Failed to load model: ${e.message}") }
            }
        }
    }

    fun sendMessage(content: String) {
        val character = _uiState.value.character ?: return
        val threadId = activeThreadId ?: return
        val lastMessage = _uiState.value.messages.lastOrNull()
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = content,
            modelReference = character.modelReference,
            parentId = lastMessage?.id,
            versionGroupId = java.util.UUID.randomUUID().toString(),
            versionIndex = 0
        )

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            // Ensure thread exists in DB before inserting message
            if (!isThreadSaved) {
                val count = chatThreadDao.getThreadCountForCharacter(character.id)
                val newThread = ChatThread(
                    id = threadId,
                    characterId = character.id,
                    title = character.name,
                    sequenceId = count + 1,
                    createdAt = startTime,
                    lastMessageAt = startTime
                )
                chatThreadDao.insertThread(newThread.toEntity())
                isThreadSaved = true
            }

            chatMessageDao.insertMessage(userMessage.toEntity(character.id, threadId))
            _uiState.update {
                it.copy(
                    messages = it.messages + userMessage,
                    isGenerating = true,
                    tokenCount = it.tokenCount + (content.length / 4),
                    versionCounts = it.versionCounts + (userMessage.versionGroupId!! to 1),
                    displayedVersions = it.displayedVersions + (userMessage.versionGroupId to 0)
                )
            }

            val loreContexts = embeddingEngine.similaritySearch(content)
            val contextInjection = if (loreContexts.isNotEmpty()) {
                "\n\n[Lore Context:\n" + loreContexts.joinToString("\n---\n") + "]"
            } else {
                ""
            }

            val thinkingHint = if (character.enableThinking) {
                "\n\nThink through the answer carefully before responding."
            } else {
                ""
            }
            val reminder = character.reminderMessage + thinkingHint + contextInjection
            var fullResponse = ""

            val history = _uiState.value.messages.dropLast(1).takeLast(10) // Send last 10 PREVIOUS messages
            generationJob = engineWrapper.sendMessage(
                text = content,
                reminder = reminder,
                history = history,
                includeThinking = character.includeThinkingInContext,
                enableThinking = character.enableThinking
            )
                .onEach { partial ->
                    fullResponse += partial
                    _uiState.update { it.copy(currentGeneratingText = fullResponse) }
                }
                .onCompletion { cause ->
                    val endTime = System.currentTimeMillis()
                    val generationTimeMs = endTime - startTime
                    
                    var finalContent = fullResponse
                    if (cause is kotlinx.coroutines.CancellationException) {
                        val hasOpenThink = (finalContent.contains("<think>") && !finalContent.contains("</think>")) ||
                                         (finalContent.contains("<|channel>thought") && !finalContent.contains("<channel|>"))
                        
                        if (hasOpenThink) {
                            val closingTag = if (finalContent.contains("<think>")) "\n</think>" else "\n<channel|>"
                            finalContent += "$closingTag\n[Stopped by user]"
                        } else {
                            finalContent += "\n[Stopped by user]"
                        }
                    }

                    if (finalContent.isNotEmpty()) {
                        withContext(kotlinx.coroutines.NonCancellable) {
                            val responseTokenCount = finalContent.length / 4
                            val tps = if (generationTimeMs > 0) responseTokenCount / (generationTimeMs / 1000f) else null

                            val modelMessage = ChatMessage(
                                role = MessageRole.MODEL,
                                content = finalContent,
                                modelReference = character.modelReference,
                                generationTimeMs = generationTimeMs,
                                tokensPerSecond = tps,
                                parentId = userMessage.id,
                                versionGroupId = java.util.UUID.randomUUID().toString(),
                                versionIndex = 0
                            )
                            chatMessageDao.insertMessage(modelMessage.toEntity(character.id, threadId))
                            chatThreadDao.updateLastMessageAt(threadId, endTime)
                            _uiState.update {
                                it.copy(
                                    messages = it.messages + modelMessage,
                                    isGenerating = false,
                                    currentGeneratingText = "",
                                    tokenCount = it.tokenCount + responseTokenCount,
                                    versionCounts = it.versionCounts + (modelMessage.versionGroupId!! to 1),
                                    displayedVersions = it.displayedVersions + (modelMessage.versionGroupId to 0)
                                )
                            }
                            checkAndSummarize(threadId, character)
                        }
                    } else {
                        _uiState.update { it.copy(isGenerating = false, currentGeneratingText = "") }
                    }
                    generationJob = null
                }
                .catch { e ->
                    _uiState.update { it.copy(isGenerating = false, currentGeneratingText = "Error: ${e.message}") }
                    generationJob = null
                }
                .launchIn(this)
        }
    }

    fun updateCharacterModelSettings(
        temp: Float,
        topP: Float,
        topK: Int,
        enableThinking: Boolean,
        enableThinkingCompatibility: Boolean,
        thinkingCompatibilityToken: String,
        includeThinkingInContext: Boolean,
        onDone: (() -> Unit)? = null
    ) {
        val character = _uiState.value.character ?: return

        viewModelScope.launch {
            characterDao.updateSamplingSettings(
                id = character.id,
                temp = temp,
                topP = topP,
                topK = topK,
                enableThinking = enableThinking,
                enableThinkingCompatibility = enableThinkingCompatibility,
                thinkingCompatibilityToken = thinkingCompatibilityToken,
                includeThinkingInContext = includeThinkingInContext
            )

            _uiState.update {
                it.copy(
                    character = character.copy(
                        temp = temp,
                        topP = topP,
                        topK = topK,
                        enableThinking = enableThinking,
                        enableThinkingCompatibility = enableThinkingCompatibility,
                        thinkingCompatibilityToken = thinkingCompatibilityToken,
                        includeThinkingInContext = includeThinkingInContext
                    ),
                    isLoadingModel = true,
                    modelError = null
                )
            }

            try {
                val modelPath = resolveModelPath(character.modelReference)
                if (modelPath.isNullOrBlank()) {
                    throw IllegalStateException("Model file not found: ${character.modelReference}")
                }

                val maxTokens = modelRepository.defaultMaxTokens.first()
                withContext(Dispatchers.IO) {
                    engineWrapper.close()
                    engineWrapper.initialize(modelPath, maxTokens = maxTokens)
                }
                _uiState.update { it.copy(maxTokens = maxTokens) }
                engineWrapper.createConversation(character.copy(
                    temp = temp,
                    topP = topP,
                    topK = topK,
                    enableThinking = enableThinking,
                    enableThinkingCompatibility = enableThinkingCompatibility,
                    thinkingCompatibilityToken = thinkingCompatibilityToken,
                    includeThinkingInContext = includeThinkingInContext
                ))

                _uiState.update { it.copy(isLoadingModel = false) }
                onDone?.invoke()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingModel = false,
                        modelError = e.message ?: "Failed to reload model"
                    )
                }
            }
        }
    }

    fun reloadCharacterModel(characterId: String) {
        viewModelScope.launch {
            val threadEntity = chatThreadDao.getThreadById(activeThreadId ?: return@launch) ?: return@launch
            val character = characterDao.getCharacterById(characterId)?.toDomain() ?: return@launch
            _uiState.update { it.copy(isLoadingModel = true, modelError = null) }
            try {
                val modelPath = resolveModelPath(character.modelReference)
                if (modelPath == null) {
                    _uiState.update { it.copy(isLoadingModel = false, modelError = "Model file not found") }
                    return@launch
                }
                val maxTokens = modelRepository.defaultMaxTokens.first()
                withContext(Dispatchers.IO) {
                    engineWrapper.close()
                    engineWrapper.initialize(modelPath, maxTokens = maxTokens)
                }
                _uiState.update { it.copy(isLoadingModel = false, maxTokens = maxTokens) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingModel = false, modelError = e.message) }
            }
        }
    }

    private suspend fun checkAndSummarize(threadId: String, character: Character) {
        val maxTokens = _uiState.value.maxTokens
        val currentTokens = _uiState.value.tokenCount
        
        // Summarize when context is ~85% full
        val threshold = (maxTokens * 0.85f).toInt()
        
        if (currentTokens > threshold) {
            val prompt = "Please summarize the story so far in 2-3 paragraphs for continuity."
            var summary = ""
            engineWrapper.sendMessage(prompt).collect { partial ->
                summary += partial
            }

            val visibleMessages = _uiState.value.messages.filter { !it.isHiddenFromAi }
            if (visibleMessages.size > 6) {
                val messagesToHide = visibleMessages.dropLast(6)
                val messageIdsToHide = messagesToHide.map { it.id }

                chatMessageDao.hideMessages(threadId, messageIdsToHide)

                val summaryMessage = ChatMessage(
                    role = MessageRole.SYSTEM,
                    content = "SUMMARY OF PAST EVENTS: $summary",
                    isHiddenFromAi = false
                )
                chatMessageDao.insertMessage(summaryMessage.toEntity(character.id, threadId))
                chatThreadDao.updateLastMessageAt(threadId, System.currentTimeMillis())

                val updatedMessages = chatMessageDao.getMessagesForThread(threadId).map { it.toDomain() }
                
                // Estimate remaining tokens (summary + last 6 messages)
                val remainingTokens = (summary.length / 4) + (updatedMessages.takeLast(6).sumOf { it.content.length / 4 })
                _uiState.update { it.copy(messages = updatedMessages, tokenCount = remainingTokens) }
            }
        }
    }

    fun switchMessageVersion(versionGroupId: String, direction: Int) {
        val currentVersion = _uiState.value.displayedVersions[versionGroupId] ?: 0
        val totalVersions = _uiState.value.versionCounts[versionGroupId] ?: 1
        val newVersion = (currentVersion + direction).coerceIn(0, totalVersions - 1)
        
        if (newVersion == currentVersion) return

        viewModelScope.launch {
            val allMessages = chatMessageDao.getMessagesForThread(activeThreadId!!).map { it.toDomain() }
            val group = allMessages.filter { (it.versionGroupId ?: it.id) == versionGroupId }
            val targetMessage = group.find { it.versionIndex == newVersion } ?: return@launch

            _uiState.update { state ->
                val newDisplayed = state.displayedVersions + (versionGroupId to newVersion)
                val newMessages = state.messages.map { 
                    if ((it.versionGroupId ?: it.id) == versionGroupId) targetMessage else it 
                }
                state.copy(
                    messages = newMessages,
                    displayedVersions = newDisplayed,
                    tokenCount = newMessages.sumOf { (it.content.length / 4).coerceAtLeast(0) }
                )
            }
        }
    }

    fun regenerateMessage(messageId: String) {
        val character = _uiState.value.character ?: return
        val threadId = activeThreadId ?: return
        val originalMessage = _uiState.value.messages.find { it.id == messageId } ?: return
        if (originalMessage.role != MessageRole.MODEL) return
        if (engineWrapper.isRawModel.value || _uiState.value.modelError != null || !engineWrapper.isInitialized()) return

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val versionGroupId = originalMessage.versionGroupId ?: originalMessage.id
            _uiState.update { it.copy(isGenerating = true, regeneratingMessageId = versionGroupId) }
            val nextVersionIndex = (_uiState.value.versionCounts[versionGroupId] ?: 1)

            // To regenerate, we need the context up to the parent of this message
            val allMessages = chatMessageDao.getMessagesForThread(threadId).map { it.toDomain() }
            val historyUpToParent = mutableListOf<ChatMessage>()
            var current: ChatMessage? = originalMessage
            while (current?.parentId != null) {
                val parent = allMessages.find { it.id == current.parentId }
                if (parent != null) {
                    historyUpToParent.add(0, parent)
                    current = parent
                } else break
            }
            // Add root message if it has no parent but is in the history
            if (current != null && current.parentId == null && !historyUpToParent.contains(current)) {
                historyUpToParent.add(0, current)
            }

            // We need to re-initialize the conversation with history up to parent
            engineWrapper.createConversation(character)
            for (msg in historyUpToParent) {
                // This is a bit inefficient, but needed to restore state
                // Actually LiteRtEngineWrapper might need a better way to restore history
                // For now, let's assume we can just send the last user message again with full context
            }
            
            val contextHistory = historyUpToParent.dropLast(1)
            val lastUserMessage = historyUpToParent.lastOrNull { it.role == MessageRole.USER } ?: return@launch
            
            var fullResponse = ""
            engineWrapper.sendMessage(
                text = lastUserMessage.content, 
                history = contextHistory,
                includeThinking = character.includeThinkingInContext,
                enableThinking = character.enableThinking
            )
                .onEach { partial ->
                    fullResponse += partial
                    _uiState.update { it.copy(currentGeneratingText = fullResponse) }
                }
                .onCompletion {
                    val endTime = System.currentTimeMillis()
                    val generationTimeMs = endTime - startTime
                    val responseTokenCount = fullResponse.length / 4
                    val tps = if (generationTimeMs > 0) responseTokenCount / (generationTimeMs / 1000f) else null

                    val modelMessage = ChatMessage(
                        role = MessageRole.MODEL,
                        content = fullResponse,
                        modelReference = character.modelReference,
                        generationTimeMs = generationTimeMs,
                        tokensPerSecond = tps,
                        parentId = originalMessage.parentId,
                        versionGroupId = versionGroupId,
                        versionIndex = nextVersionIndex
                    )
                    chatMessageDao.insertMessage(modelMessage.toEntity(character.id, threadId))
                    
                    _uiState.update { state ->
                        val newMessages = state.messages.map { 
                            if ((it.versionGroupId ?: it.id) == versionGroupId) modelMessage else it 
                        }
                        state.copy(
                            messages = newMessages,
                            isGenerating = false,
                            currentGeneratingText = "",
                            regeneratingMessageId = null,
                            tokenCount = newMessages.sumOf { (it.content.length / 4).coerceAtLeast(0) },
                            versionCounts = state.versionCounts + (versionGroupId to nextVersionIndex + 1),
                            displayedVersions = state.displayedVersions + (versionGroupId to nextVersionIndex)
                        )
                    }
                }
                .collect()
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        val character = _uiState.value.character ?: return
        val threadId = activeThreadId ?: return
        val originalMessage = _uiState.value.messages.find { it.id == messageId } ?: return

        viewModelScope.launch {
            val versionGroupId = originalMessage.versionGroupId ?: originalMessage.id
            val nextVersionIndex = (_uiState.value.versionCounts[versionGroupId] ?: 1)

            val updatedMessage = originalMessage.copy(
                id = java.util.UUID.randomUUID().toString(),
                content = newContent,
                timestamp = System.currentTimeMillis(),
                versionIndex = nextVersionIndex,
                versionGroupId = versionGroupId
            )

            chatMessageDao.insertMessage(updatedMessage.toEntity(character.id, threadId))

            _uiState.update { state ->
                val newMessages = state.messages.map { 
                    if ((it.versionGroupId ?: it.id) == versionGroupId) updatedMessage else it 
                }
                state.copy(
                    messages = newMessages,
                    tokenCount = newMessages.sumOf { (it.content.length / 4).coerceAtLeast(0) },
                    versionCounts = state.versionCounts + (versionGroupId to nextVersionIndex + 1),
                    displayedVersions = state.displayedVersions + (versionGroupId to nextVersionIndex)
                )
            }
        }
    }

    fun forkThread(messageId: String) {
        val character = _uiState.value.character ?: return
        val threadId = activeThreadId ?: return
        
        viewModelScope.launch {
            val allMessages = chatMessageDao.getMessagesForThread(threadId).map { it.toDomain() }
            val index = allMessages.indexOfFirst { it.id == messageId }
            if (index == -1) return@launch
            
            val messagesToCopy = allMessages.take(index + 1)
            val newThreadId = java.util.UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()
            
            val newThread = ChatThread(
                id = newThreadId,
                characterId = character.id,
                title = "Fork of ${_uiState.value.threadTitle}",
                createdAt = startTime,
                lastMessageAt = startTime,
                sequenceId = chatThreadDao.getThreadCountForCharacter(character.id) + 1
            )
            
            chatThreadDao.insertThread(newThread.toEntity())
            
            for (msg in messagesToCopy) {
                chatMessageDao.insertMessage(msg.toEntity(character.id, newThreadId))
            }
            
            // Navigate to new thread (handled by UI via a state change or callback)
            // For now, we'll just update the activeThreadId and reload
            loadConversation(newThreadId)
        }
    }

    fun renameThread(newTitle: String) {
        val threadId = activeThreadId ?: return
        viewModelScope.launch {
            chatThreadDao.updateThreadTitle(threadId, newTitle)
            _uiState.update { it.copy(threadTitle = newTitle) }
        }
    }

    fun deleteMessage(messageId: String, mode: DeletionMode) {
        val threadId = activeThreadId ?: return
        viewModelScope.launch {
            val message = chatMessageDao.getMessageById(messageId) ?: return@launch
            
            if (mode == DeletionMode.ONLY_THIS) {
                chatMessageDao.deleteMessageById(messageId)
            } else {
                chatMessageDao.deleteMessagesAfter(threadId, message.timestamp)
            }
            
            loadConversation(threadId)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
