package org.eu.nl.syu.charchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eu.nl.syu.charchat.data.AllowedModel
import org.eu.nl.syu.charchat.data.DefaultCharacters
import org.eu.nl.syu.charchat.data.ChatThread
import org.eu.nl.syu.charchat.data.ModelManager
import org.eu.nl.syu.charchat.data.ModelRepository
import org.eu.nl.syu.charchat.runtime.LiteRtEngineWrapper
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.data.local.CharacterDao
import org.eu.nl.syu.charchat.data.local.ChatThreadDao
import org.eu.nl.syu.charchat.data.local.toDomain
import com.google.ai.edge.litertlm.Backend
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

data class HomeUiState(
    val characters: List<Character> = emptyList(),
    val chatThreads: List<ChatThread> = emptyList(),
    val downloadedModels: List<File> = emptyList(),
    val availableModels: List<AllowedModel> = emptyList(),
    val selectedModel: String? = null,
    val isModelLoaded: Boolean = false,
    val isModelLoading: Boolean = false,
    val isLoading: Boolean = false,
    val notification: String? = null,
    val preferredBackend: String = "Automatic",
    val defaultMaxTokens: Int = 4096,
    val experimentalNpuEnabled: Boolean = false,
    val autoLoadChatModel: Boolean = false,
    val failedModels: Set<String> = emptySet()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val characterDao: CharacterDao,
    private val chatThreadDao: ChatThreadDao,
    private val modelRepository: ModelRepository,
    private val modelManager: ModelManager,
    private val engineWrapper: LiteRtEngineWrapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeEngineState()
        loadCharacters()
        loadThreads()
        loadCachedModels()
        observeSettings()
        observeFailedModels()
    }

    private fun observeEngineState() {
        viewModelScope.launch {
            engineWrapper.loadedModelPath.collect { loadedModelPath ->
                _uiState.update { state ->
                    state.copy(
                        selectedModel = loadedModelPath?.let { File(it).name },
                        isModelLoaded = loadedModelPath != null,
                        isModelLoading = false
                    )
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            modelRepository.preferredBackend.collect { backend ->
                _uiState.update { it.copy(preferredBackend = backend) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            modelRepository.defaultMaxTokens.collect { tokens ->
                _uiState.update { it.copy(defaultMaxTokens = tokens) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            modelRepository.experimentalNpuEnabled.collect { enabled ->
                _uiState.update { it.copy(experimentalNpuEnabled = enabled) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            modelRepository.autoLoadChatModel.collect { enabled ->
                _uiState.update { it.copy(autoLoadChatModel = enabled) }
            }
        }
    }

    private fun observeFailedModels() {
        viewModelScope.launch(Dispatchers.IO) {
            modelRepository.failedModels.collect { failed ->
                _uiState.update { it.copy(failedModels = failed) }
            }
        }
    }

    fun retryModel(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            modelRepository.removeFailedModel(file.name)
            val hash = modelRepository.hashOf(file)
            if (hash.isNotEmpty()) {
                modelRepository.removeFromBlacklist(hash)
            }
            // Attempt to reload the model
            selectModel(file)
        }
    }

    fun setPreferredBackend(backend: String) {
        viewModelScope.launch {
            modelRepository.setPreferredBackend(backend)
            reloadSelectedModelIfNeeded()
        }
    }

    fun setDefaultMaxTokens(tokens: Int) {
        viewModelScope.launch {
            modelRepository.setDefaultMaxTokens(tokens)
            reloadSelectedModelIfNeeded()
        }
    }

    fun setAutoLoadChatModel(enabled: Boolean) {
        viewModelScope.launch {
            modelRepository.setAutoLoadChatModel(enabled)
        }
    }

    fun loadCharacters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val characters = withContext(Dispatchers.IO) {
                characterDao.getAllCharacters().map { it.toDomain() }
            }
            _uiState.update {
                it.copy(
                    characters = characters,
                    isLoading = false
                )
            }
        }
    }

    fun loadThreads() {
        viewModelScope.launch(Dispatchers.IO) {
            chatThreadDao.getAllThreads().collectLatest { threads ->
                _uiState.update { it.copy(chatThreads = threads.map { thread -> thread.toDomain() }) }
            }
        }
    }

    fun createThreadForCharacter(character: Character): ChatThread {
        val now = System.currentTimeMillis()
        return ChatThread(
            characterId = character.id,
            title = character.name,
            createdAt = now,
            lastMessageAt = now
        )
    }

    fun refreshModels() {
        viewModelScope.launch(Dispatchers.IO) {
            val availableModels = modelRepository.refreshAvailableModelsFromRemote()
            updateModelsList(availableModels)
        }
    }

    fun reloadLocalModels() {
        viewModelScope.launch(Dispatchers.IO) {
            val availableModels = modelRepository.getAvailableModels()
            updateModelsList(availableModels)
        }
    }

    private suspend fun updateModelsList(availableModels: List<AllowedModel>) {
        val localFiles = modelManager.getLocalModels()
            .filter { (it.name.endsWith(".litertlm") || it.name.endsWith(".tflite")) }
        val models = localFiles
            .filterNot { isEmbeddingModel(it, availableModels) }
            .sortedWith(
                compareBy<File> { it.name.lowercase() }
            )
        _uiState.update { it.copy(downloadedModels = models, availableModels = availableModels) }
    }

    private fun loadCachedModels() {
        viewModelScope.launch(Dispatchers.IO) {
            val availableModels = modelRepository.getAvailableModels()
            updateModelsList(availableModels)
        }
    }

    private suspend fun isBlacklisted(file: File): Boolean {
        val hash = modelRepository.hashOf(file)
        if (hash.isNotEmpty()) {
            return modelRepository.isModelBlacklisted(hash)
        }
        return false
    }

    private fun isEmbeddingModel(file: File, availableModels: List<AllowedModel>): Boolean {
        val matchingModel = availableModels.firstOrNull { modelRepository.getDownloadFileName(it) == file.name }
        return matchingModel?.taskTypes?.contains("embedding") == true || file.name.contains("embedding", ignoreCase = true)
    }

    fun selectModel(file: File) {
        viewModelScope.launch {
            if (isEmbeddingModel(file, _uiState.value.availableModels)) {
                _uiState.update {
                    it.copy(
                        isModelLoading = false,
                        notification = "Embedding models cannot be used for chat. Select a LiteRT LM model instead."
                    )
                }
                return@launch
            }

            loadModel(file = file)
        }
    }

    private suspend fun reloadSelectedModelIfNeeded() {
        val selectedModelName = _uiState.value.selectedModel ?: return
        val currentFile = modelManager.getLocalModels().firstOrNull { it.name == selectedModelName }
            ?: return
        if (_uiState.value.isModelLoaded) {
            loadModel(file = currentFile)
        }
    }

    private suspend fun loadModel(file: File) {
        _uiState.update { it.copy(isModelLoading = true) }

        val backend = when (_uiState.value.preferredBackend) {
            "GPU" -> Backend.GPU()
            "CPU" -> Backend.CPU()
            "NPU" -> Backend.NPU(modelManager.getModelsDir().absolutePath)
            else -> null
        }

        try {
            withContext(Dispatchers.IO) {
                engineWrapper.initialize(
                    modelPath = file.absolutePath,
                    preferredBackend = backend,
                    maxTokens = _uiState.value.defaultMaxTokens,
                    onFallback = { message ->
                        _uiState.update { it.copy(notification = message) }
                    }
                )
            }
            characterDao.updateModelReference(org.eu.nl.syu.charchat.data.DefaultCharacters.ASSISTANT_CHARACTER_ID, file.absolutePath)
            modelRepository.removeFailedModel(file.name)
            val hash = modelRepository.hashOf(file)
            if (hash.isNotEmpty()) {
                modelRepository.removeFromBlacklist(hash)
                // Update cached model with hash
                val modelId = _uiState.value.availableModels
                    .find { it.localFileName == file.name || it.modelFile == file.name }
                    ?.modelId
                if (modelId != null) {
                    modelRepository.updateCachedModelHash(modelId, hash)
                }
            }
            _uiState.update { it.copy(isModelLoading = false, selectedModel = file.name, isModelLoaded = true, notification = null) }
        } catch (e: Exception) {
            val msg = when {
                isInputTensorMissing(e) -> {
                    modelRepository.addFailedModel(file.name)
                    "This model failed to load.\n\nTip: Try changing the hardware mode or reducing the context length."
                }
                isUnsupportedChatModel(file.name) -> {
                    "This model cannot be used for chat. Select a LiteRT LM model instead."
                }
                else -> {
                    modelRepository.addFailedModel(file.name)
                    "Failed to initialize model: ${e.message}\n\nTip: Try changing the hardware mode or reducing the context length."
                }
            }
            _uiState.update {
                it.copy(
                    isModelLoading = false,
                    isModelLoaded = false,
                    selectedModel = null,
                    notification = msg
                )
            }
            return
        }
    }

    private fun isInputTensorMissing(e: Exception): Boolean {
        val msg = e.message ?: return false
        return msg.contains("Input tensor not found", ignoreCase = true) ||
                msg.contains("NOT_FOUND", ignoreCase = true) ||
                msg.contains("tensor not found", ignoreCase = true)
    }

    private fun isUnsupportedChatModel(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".tflite") && !lower.contains("chat") && !lower.contains("lm")
    }

    fun markCharacterOpened(characterId: String) {
        viewModelScope.launch {
            val openedAt = System.currentTimeMillis()
            characterDao.updateLastUsedAt(characterId, openedAt)
            _uiState.update { state ->
                state.copy(
                    characters = state.characters.map { character ->
                        if (character.id == characterId) character.copy(lastUsedAt = openedAt) else character
                    }.sortedWith(
                        compareByDescending<Character> { it.lastUsedAt }
                            .thenBy { it.name.lowercase() }
                    )
                )
            }
            loadCharacters()
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notification = null) }
    }

    fun unloadModel() {
        viewModelScope.launch {
            engineWrapper.close()
            _uiState.update {
                it.copy(
                    selectedModel = null,
                    isModelLoaded = false,
                    isModelLoading = false
                )
            }
        }
    }
}
