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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eu.nl.syu.hearth.data.Character
import org.eu.nl.syu.hearth.data.ModelRepository
import org.eu.nl.syu.hearth.data.local.CharacterDao
import org.eu.nl.syu.hearth.data.local.toDomain
import org.eu.nl.syu.hearth.runtime.LiteRtEngineWrapper
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

            _uiState.update {
                it.copy(
                    preferredBackend = modelRepository.preferredBackend.first(),
                    defaultMaxTokens = modelRepository.defaultMaxTokens.first(),
                    experimentalNpuEnabled = modelRepository.experimentalNpuEnabled.first()
                )
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

    fun updateSettings(
        backend: String,
        maxTokens: Int,
        characterId: String?,
        temp: Float,
        topP: Float,
        topK: Int,
        enableThinking: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Save global settings
                modelRepository.setPreferredBackend(backend)
                modelRepository.setDefaultMaxTokens(maxTokens)
                
                // Save character settings if applicable
                if (characterId != null) {
                    characterDao.updateSamplingSettings(
                        id = characterId,
                        temp = temp,
                        topP = topP,
                        topK = topK,
                        enableThinking = enableThinking
                    )
                }
                
                // Update local state
                _uiState.update { state ->
                    state.copy(
                        preferredBackend = backend,
                        defaultMaxTokens = maxTokens,
                        character = state.character?.copy(
                            temp = temp,
                            topP = topP,
                            topK = topK,
                            enableThinking = enableThinking
                        )
                    )
                }
                
                // Reload engine with new settings
                reloadModel()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to update settings") }
            }
        }
    }

    private suspend fun reloadModel() {
        val modelPath = engineWrapper.getLoadedModelPath() ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val maxTokens = modelRepository.defaultMaxTokens.first()
            withContext(Dispatchers.IO) {
                engineWrapper.close()
                engineWrapper.initialize(modelPath, maxTokens = maxTokens)
            }
            _uiState.update { it.copy(isLoading = false, defaultMaxTokens = maxTokens) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to reload model") }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
