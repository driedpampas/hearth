package org.eu.nl.syu.charchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.data.ChatMessage
import org.eu.nl.syu.charchat.data.MessageRole
import org.eu.nl.syu.charchat.data.local.CharacterDao
import org.eu.nl.syu.charchat.data.local.ChatMessageDao
import org.eu.nl.syu.charchat.data.local.toDomain
import org.eu.nl.syu.charchat.data.local.toEntity
import org.eu.nl.syu.charchat.runtime.EmbeddingEngine
import org.eu.nl.syu.charchat.runtime.LiteRtEngineWrapper
import javax.inject.Inject

data class ChatUiState(
    val character: Character? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val currentGeneratingText: String = "",
    val tokenCount: Int = 0,
    val maxTokens: Int = 4096
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val characterDao: CharacterDao,
    private val chatMessageDao: ChatMessageDao,
    private val engineWrapper: LiteRtEngineWrapper,
    private val embeddingEngine: EmbeddingEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun loadCharacter(characterId: String) {
        viewModelScope.launch {
            val characterEntity = characterDao.getCharacterById(characterId)
            val character = characterEntity?.toDomain()
            if (character != null) {
                val openedAt = System.currentTimeMillis()
                characterDao.updateLastUsedAt(character.id, openedAt)
                val messages = chatMessageDao.getMessagesForCharacter(characterId).map { it.toDomain() }
                _uiState.update { it.copy(character = character.copy(lastUsedAt = openedAt), messages = messages) }
                
                // Initialize engine if needed
                engineWrapper.initialize(character.modelReference)
                engineWrapper.createConversation(character).collect()
            }
        }
    }

    fun sendMessage(content: String) {
        val character = _uiState.value.character ?: return
        val userMessage = ChatMessage(role = MessageRole.USER, content = content)
        
        viewModelScope.launch {
            // Save user message
            chatMessageDao.insertMessage(userMessage.toEntity(character.id))
            _uiState.update { it.copy(
                messages = it.messages + userMessage,
                isGenerating = true,
                tokenCount = it.tokenCount + (content.length / 4) // Simple heuristic
            ) }

            // Retrieve RAG Context
            val loreContexts = embeddingEngine.similaritySearch(content)
            val contextInjection = if (loreContexts.isNotEmpty()) {
                "\n\n[Lore Context:\n" + loreContexts.joinToString("\n---\n") + "]"
            } else {
                ""
            }

            val reminder = character.reminderMessage + contextInjection
            var fullResponse = ""
            
            engineWrapper.sendMessage(content, reminder)
                .onEach { partial ->
                    fullResponse += partial
                    _uiState.update { it.copy(currentGeneratingText = fullResponse) }
                }
                .onCompletion {
                    val modelMessage = ChatMessage(role = MessageRole.MODEL, content = fullResponse)
                    chatMessageDao.insertMessage(modelMessage.toEntity(character.id))
                    _uiState.update { 
                        it.copy(
                            messages = it.messages + modelMessage,
                            isGenerating = false,
                            currentGeneratingText = "",
                            tokenCount = it.tokenCount + (fullResponse.length / 4) // Simple heuristic
                        )
                    }
                    
                    // Check context window and summarize if needed (TODO)
                    checkAndSummarize(character)
                }
                .catch { e ->
                    _uiState.update { it.copy(isGenerating = false, currentGeneratingText = "Error: ${e.message}") }
                }
                .collect()
        }
    }

    private suspend fun checkAndSummarize(character: Character) {
        if (_uiState.value.tokenCount > 3500) { // Approx 85% of 4096
            val prompt = "Please summarize the story so far in 2-3 paragraphs for continuity."
            var summary = ""
            engineWrapper.sendMessage(prompt).collect { partial ->
                summary += partial
            }
            
            val visibleMessages = _uiState.value.messages.filter { !it.isHiddenFromAi }
            // Keep the last 6 messages for immediate context, hide the rest
            if (visibleMessages.size > 6) {
                val messagesToHide = visibleMessages.dropLast(6)
                val messageIdsToHide = messagesToHide.map { it.id }
                
                chatMessageDao.hideMessages(character.id, messageIdsToHide)
                
                val summaryMessage = ChatMessage(
                    role = MessageRole.SYSTEM,
                    content = "SUMMARY OF PAST EVENTS: $summary",
                    isHiddenFromAi = false
                )
                chatMessageDao.insertMessage(summaryMessage.toEntity(character.id))
                
                // Refresh UI state with updated messages
                val updatedMessages = chatMessageDao.getMessagesForCharacter(character.id).map { it.toDomain() }
                _uiState.update { it.copy(messages = updatedMessages, tokenCount = 1500) } // Reset token count heuristic
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        engineWrapper.close()
    }
}
