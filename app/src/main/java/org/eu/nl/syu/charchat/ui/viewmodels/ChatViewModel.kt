package org.eu.nl.syu.charchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.data.ChatMessage
import org.eu.nl.syu.charchat.data.MessageRole
import org.eu.nl.syu.charchat.data.local.CharacterDao
import org.eu.nl.syu.charchat.data.local.ChatMessageDao
import org.eu.nl.syu.charchat.data.local.toDomain
import org.eu.nl.syu.charchat.data.local.toEntity
import org.eu.nl.syu.charchat.runtime.LiteRtEngineWrapper
import java.util.*
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
    private val engineWrapper: LiteRtEngineWrapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun loadCharacter(characterId: String) {
        viewModelScope.launch {
            val characterEntity = characterDao.getCharacterById(characterId)
            val character = characterEntity?.toDomain()
            if (character != null) {
                val messages = chatMessageDao.getMessagesForCharacter(characterId).map { it.toDomain() }
                _uiState.update { it.copy(character = character, messages = messages) }
                
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

            val reminder = character.reminderMessage
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
            
            // In a real app, we'd replace old messages with this summary.
            // For now, we'll just add it as a system message.
            val summaryMessage = ChatMessage(
                role = MessageRole.SYSTEM,
                content = "SUMMARY: $summary",
                isHiddenFromAi = false
            )
            chatMessageDao.insertMessage(summaryMessage.toEntity(character.id))
            _uiState.update { it.copy(messages = it.messages + summaryMessage, tokenCount = 500) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engineWrapper.close()
    }
}
