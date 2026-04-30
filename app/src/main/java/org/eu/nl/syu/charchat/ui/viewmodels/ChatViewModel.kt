package org.eu.nl.syu.charchat.ui.viewmodels

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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.data.ChatMessage
import org.eu.nl.syu.charchat.data.ChatThread
import org.eu.nl.syu.charchat.data.MessageRole
import org.eu.nl.syu.charchat.data.ModelManager
import org.eu.nl.syu.charchat.data.ModelRepository
import org.eu.nl.syu.charchat.data.local.CharacterDao
import org.eu.nl.syu.charchat.data.local.ChatMessageDao
import org.eu.nl.syu.charchat.data.local.ChatThreadDao
import org.eu.nl.syu.charchat.data.local.toDomain
import org.eu.nl.syu.charchat.data.local.toEntity
import org.eu.nl.syu.charchat.runtime.EmbeddingEngine
import org.eu.nl.syu.charchat.runtime.LiteRtEngineWrapper
import java.io.File
import javax.inject.Inject

data class ChatUiState(
    val character: Character? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val currentGeneratingText: String = "",
    val tokenCount: Int = 0,
    val maxTokens: Int = 4096,
    val modelError: String? = null,
    val isLoadingModel: Boolean = false,
    val threadTitle: String? = null
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

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            val threadEntity = chatThreadDao.getThreadById(conversationId)
            isThreadSaved = threadEntity != null
            val character = when {
                threadEntity != null -> characterDao.getCharacterById(threadEntity.characterId)?.toDomain()
                else -> characterDao.getCharacterById(conversationId)?.toDomain()
            } ?: return@launch

            val thread = threadEntity?.toDomain() ?: ChatThread(
                characterId = character.id,
                title = character.name
            )
            val threadId = thread.id
            activeThreadId = threadId

            val modelPath = resolveModelPath(character.modelReference)
            val autoLoadEnabled = modelRepository.autoLoadChatModel.first()
            if (!engineWrapper.isInitialized() && !autoLoadEnabled) {
                _uiState.update {
                    it.copy(
                        character = character,
                        threadTitle = thread.title,
                        modelError = if (character.modelReference.isBlank()) {
                            "Select a model first."
                        } else {
                            "Model file not found: ${character.modelReference}.\nDid you download it?"
                        }
                    )
                }
                return@launch
            }

            if (modelPath == null && !engineWrapper.isInitialized()) {
                _uiState.update {
                    it.copy(
                        character = character,
                        threadTitle = thread.title,
                        modelError = if (character.modelReference.isBlank()) {
                            "Select a model first."
                        } else {
                            "Model file not found: ${character.modelReference}.\nDid you download it?"
                        }
                    )
                }
                return@launch
            }

            if (!engineWrapper.isInitialized() && autoLoadEnabled) {
                if (modelPath == null) {
                    _uiState.update {
                        it.copy(
                            character = character,
                            threadTitle = thread.title,
                            modelError = "Select a model first."
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(character = character, threadTitle = thread.title, isLoadingModel = true, modelError = null) }

                val modelFile = File(modelPath)
                try {
                    withContext(Dispatchers.IO) {
                        engineWrapper.initialize(modelPath)
                    }
                } catch (e: Exception) {
                    val msg = when {
                        isInputTensorMissing(e) -> {
                            modelRepository.quarantineModel(modelFile)
                            "This model is corrupted/incompatible and has been removed. Please select a compatible LiteRT LM chat model."
                        }
                        isUnsupportedChatModel(character.modelReference) -> {
                            "This model cannot be used for chat. Select a LiteRT LM model instead."
                        }
                        else -> {
                            "Failed to initialize the model: ${e.message}"
                        }
                    }
                    _uiState.update {
                        it.copy(
                            character = character,
                            threadTitle = thread.title,
                            modelError = msg,
                            isLoadingModel = false
                        )
                    }
                    return@launch
                }
            }

            val openedAt = System.currentTimeMillis()
            characterDao.updateLastUsedAt(character.id, openedAt)
            val messages = chatMessageDao.getMessagesForThread(threadId).map { it.toDomain() }
            
            _uiState.update {
                it.copy(
                    character = character.copy(lastUsedAt = openedAt),
                    messages = messages,
                    modelError = null,
                    isLoadingModel = false,
                    tokenCount = messages.sumOf { message -> (message.content.length / 4).coerceAtLeast(0) },
                    threadTitle = thread.title
                )
            }

            engineWrapper.createConversation(character).collect()
        }
    }

    // Removed immediate createNewThread insertion to avoid empty threads in history.

    private fun isUnsupportedChatModel(modelReference: String): Boolean {
        val lower = modelReference.lowercase()
        return lower.endsWith(".tflite") && !lower.contains("chat") && !lower.contains("lm")
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

    fun sendMessage(content: String) {
        val character = _uiState.value.character ?: return
        val threadId = activeThreadId ?: return
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = content,
            modelReference = character.modelReference
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
                    tokenCount = it.tokenCount + (content.length / 4)
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

            engineWrapper.sendMessage(content, reminder)
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
                        tokensPerSecond = tps
                    )
                    chatMessageDao.insertMessage(modelMessage.toEntity(character.id, threadId))
                    chatThreadDao.updateLastMessageAt(threadId, endTime)
                    _uiState.update {
                        it.copy(
                            messages = it.messages + modelMessage,
                            isGenerating = false,
                            currentGeneratingText = "",
                            tokenCount = it.tokenCount + responseTokenCount
                        )
                    }

                    checkAndSummarize(threadId, character)
                }
                .catch { e ->
                    _uiState.update { it.copy(isGenerating = false, currentGeneratingText = "Error: ${e.message}") }
                }
                .collect()
        }
    }

    fun updateCharacterModelSettings(
        temp: Float,
        topP: Float,
        topK: Int,
        enableThinking: Boolean,
        onDone: (() -> Unit)? = null
    ) {
        val character = _uiState.value.character ?: return

        viewModelScope.launch {
            characterDao.updateSamplingSettings(
                id = character.id,
                temp = temp,
                topP = topP,
                topK = topK,
                enableThinking = enableThinking
            )

            _uiState.update {
                it.copy(
                    character = character.copy(
                        temp = temp,
                        topP = topP,
                        topK = topK,
                        enableThinking = enableThinking
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

                withContext(Dispatchers.IO) {
                    engineWrapper.close()
                    engineWrapper.initialize(modelPath)
                }
                engineWrapper.createConversation(character.copy(
                    temp = temp,
                    topP = topP,
                    topK = topK,
                    enableThinking = enableThinking
                )).collect()

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
                withContext(Dispatchers.IO) {
                    engineWrapper.close()
                    engineWrapper.initialize(modelPath)
                }
                _uiState.update { it.copy(isLoadingModel = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingModel = false, modelError = e.message) }
            }
        }
    }

    private suspend fun checkAndSummarize(threadId: String, character: Character) {
        if (_uiState.value.tokenCount > 3500) {
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
                _uiState.update { it.copy(messages = updatedMessages, tokenCount = 1500) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
