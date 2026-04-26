package org.eu.nl.syu.charchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.eu.nl.syu.charchat.data.AllowedModel
import org.eu.nl.syu.charchat.data.ModelManager
import org.eu.nl.syu.charchat.data.ModelRepository
import org.eu.nl.syu.charchat.runtime.LiteRtEngineWrapper
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.data.local.CharacterDao
import org.eu.nl.syu.charchat.data.local.toDomain
import com.google.ai.edge.litertlm.Backend
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class HomeUiState(
    val characters: List<Character> = emptyList(),
    val downloadedModels: List<File> = emptyList(),
    val selectedModel: String? = null,
    val isModelLoading: Boolean = false,
    val isLoading: Boolean = false,
    val notification: String? = null,
    val preferredBackend: String = "Automatic",
    val defaultMaxTokens: Int = 4096,
    val experimentalNpuEnabled: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val characterDao: CharacterDao,
    private val modelRepository: ModelRepository,
    private val modelManager: ModelManager,
    private val engineWrapper: LiteRtEngineWrapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCharacters()
        refreshModels()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            modelRepository.preferredBackend.collect { backend ->
                _uiState.update { it.copy(preferredBackend = backend) }
            }
        }
        viewModelScope.launch {
            modelRepository.defaultMaxTokens.collect { tokens ->
                _uiState.update { it.copy(defaultMaxTokens = tokens) }
            }
        }
        viewModelScope.launch {
            modelRepository.experimentalNpuEnabled.collect { enabled ->
                _uiState.update { it.copy(experimentalNpuEnabled = enabled) }
            }
        }
    }

    fun setPreferredBackend(backend: String) {
        viewModelScope.launch {
            modelRepository.setPreferredBackend(backend)
        }
    }

    fun setDefaultMaxTokens(tokens: Int) {
        viewModelScope.launch {
            modelRepository.setDefaultMaxTokens(tokens)
        }
    }

    fun loadCharacters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val characters = characterDao.getAllCharacters().map { it.toDomain() }
            _uiState.update { it.copy(characters = characters, isLoading = false) }
        }
    }

    fun refreshModels() {
        viewModelScope.launch {
            val models = modelManager.getLocalModels().filter { it.name.endsWith(".tflite") || it.name.endsWith(".litertlm") }
            _uiState.update { it.copy(downloadedModels = models) }
        }
    }

    fun selectModel(file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isModelLoading = true, selectedModel = file.name) }
            
            val backend = when (_uiState.value.preferredBackend) {
                "GPU" -> Backend.GPU()
                "CPU" -> Backend.CPU()
                "NPU" -> Backend.NPU(modelManager.getModelsDir().absolutePath) // nativeLibraryDir is better but this works for demo
                else -> null
            }

            engineWrapper.initialize(
                modelPath = file.absolutePath,
                preferredBackend = backend,
                maxTokens = _uiState.value.defaultMaxTokens,
                onFallback = { message ->
                    _uiState.update { it.copy(notification = message) }
                }
            )
            _uiState.update { it.copy(isModelLoading = false) }
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notification = null) }
    }
}
