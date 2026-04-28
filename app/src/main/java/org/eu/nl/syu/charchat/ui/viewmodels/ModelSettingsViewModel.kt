package org.eu.nl.syu.charchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.data.ModelRepository
import org.eu.nl.syu.charchat.data.local.CharacterDao
import org.eu.nl.syu.charchat.data.local.toDomain
import org.eu.nl.syu.charchat.runtime.LiteRtEngineWrapper
import javax.inject.Inject

data class ModelSettingsUiState(
    val character: Character? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val preferredBackend: String = "Automatic",
    val defaultMaxTokens: Int = 4096,
    val experimentalNpuEnabled: Boolean = false,
    val temp: Float = 0.8f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val enableThinking: Boolean = false
)

@HiltViewModel
class ModelSettingsViewModel @Inject constructor(
    private val characterDao: CharacterDao,
    private val modelRepository: ModelRepository,
    private val engineWrapper: LiteRtEngineWrapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSettingsUiState())
    val uiState: StateFlow<ModelSettingsUiState> = _uiState.asStateFlow()

    fun loadSettings(characterId: String?) {
        viewModelScope.launch {
            val character = characterId?.let {
                characterDao.getCharacterById(it)?.toDomain()
            }

            launch {
                modelRepository.preferredBackend.collect { backend ->
                    _uiState.update { it.copy(preferredBackend = backend) }
                }
            }
            launch {
                modelRepository.defaultMaxTokens.collect { tokens ->
                    _uiState.update { it.copy(defaultMaxTokens = tokens) }
                }
            }
            launch {
                modelRepository.experimentalNpuEnabled.collect { enabled ->
                    _uiState.update { it.copy(experimentalNpuEnabled = enabled) }
                }
            }

            if (character != null) {
                _uiState.update {
                    it.copy(
                        character = character,
                        temp = character.temp,
                        topP = character.topP,
                        topK = character.topK,
                        enableThinking = character.enableThinking
                    )
                }
            }
        }
    }

    fun updateBackend(backend: String) {
        viewModelScope.launch {
            modelRepository.setPreferredBackend(backend)
            reloadModel()
        }
    }

    fun updateMaxTokens(tokens: Int) {
        viewModelScope.launch {
            modelRepository.setDefaultMaxTokens(tokens)
            reloadModel()
        }
    }

    fun updateCharacterSettings(temp: Float, topP: Float, topK: Int, enableThinking: Boolean) {
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
                    )
                )
            }
            reloadModel()
        }
    }

    private suspend fun reloadModel() {
        val modelPath = engineWrapper.getLoadedModelPath() ?: return
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            withContext(Dispatchers.IO) {
                engineWrapper.close()
                engineWrapper.initialize(modelPath)
            }
            _uiState.update { it.copy(isLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to reload model") }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
