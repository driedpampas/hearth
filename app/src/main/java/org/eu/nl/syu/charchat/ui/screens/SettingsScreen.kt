package org.eu.nl.syu.charchat.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eu.nl.syu.charchat.data.DownloadState
import org.eu.nl.syu.charchat.data.ModelManager
import org.eu.nl.syu.charchat.data.AuthRepository
import org.eu.nl.syu.charchat.data.AuthToken
import org.eu.nl.syu.charchat.common.ProjectConfig
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.ResponseTypeValues
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import javax.inject.Inject
import androidx.core.net.toUri

// --- Main Settings Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGeneral: () -> Unit,
    onNavigateToModels: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                ListItem(
                    headlineContent = { Text("General") },
                    leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToGeneral() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Models") },
                    leadingContent = { Icon(Icons.Default.ModelTraining, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToModels() }
                )
            }
        }
    }
}

// --- General Settings Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGeneralScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text("System Default") },
                trailingContent = { Switch(checked = true, onCheckedChange = {}) }
            )
            ListItem(
                headlineContent = { Text("Haptics") },
                supportingContent = { Text("Vibrate on message") },
                trailingContent = { Switch(checked = true, onCheckedChange = {}) }
            )
        }
    }
}

// --- Models Settings Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModelsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLiteRt: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Models") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ListItem(
                headlineContent = { Text("LiteRT Models") },
                supportingContent = { Text("Optimized for Android") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToLiteRt() }
            )
            ListItem(
                headlineContent = { Text("GGUF Models") },
                supportingContent = { Text("Coming Soon") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier.alpha(0.5f)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            val hfViewModel: ModelsViewModel = hiltViewModel()
            val isLoggedIn by hfViewModel.isLoggedIn.collectAsStateWithLifecycle()
            
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                hfViewModel.handleAuthResult(result)
            }
            val context = LocalContext.current

            ListItem(
                headlineContent = { Text("HuggingFace Account") },
                supportingContent = { Text(if (isLoggedIn) "Logged in" else "Not logged in") },
                leadingContent = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                trailingContent = {
                    if (isLoggedIn) {
                        TextButton(onClick = { hfViewModel.logout() }) {
                            Text("Logout", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Button(onClick = {
                            val authRequest = hfViewModel.getAuthorizationRequest()
                            val authService = AuthorizationService(context)
                            val authIntent = authService.getAuthorizationRequestIntent(authRequest)
                            launcher.launch(authIntent)
                            authService.dispose()
                        }) {
                            Text("Login")
                        }
                    }
                }
            )
        }
    }
}

// --- LiteRT Models Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLiteRtModelsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LiteRT Models") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(uiState.availableModels) { model ->
                val isDownloaded = uiState.downloadedModels.contains(model.fileName)
                val progress = uiState.downloadProgress[model.fileName]
                val error = uiState.downloadErrors[model.fileName]
                
                ListItem(
                    headlineContent = { Text(model.name) },
                    supportingContent = { 
                        if (error != null) {
                            Text(error, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(model.description)
                        }
                    },
                    leadingContent = { Icon(Icons.Default.ModelTraining, contentDescription = null) },
                    trailingContent = {
                        if (isDownloaded) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = MaterialTheme.colorScheme.primary)
                        } else if (progress != null && progress < 100) {
                            CircularProgressIndicator(progress = progress / 100f, modifier = Modifier.size(24.dp))
                        } else {
                            IconButton(onClick = { viewModel.downloadModel(model) }) {
                                Icon(if (error != null) Icons.Default.Refresh else Icons.Default.Download, contentDescription = "Download")
                            }
                        }
                    }
                )
            }
        }
    }
}

// --- ViewModel and Helper Classes ---

data class ModelInfo(
    val name: String,
    val description: String,
    val url: String,
    val fileName: String
)

data class ModelsUiState(
    val availableModels: List<ModelInfo> = listOf(
        ModelInfo("Gemma 2B (LiteRT)", "Optimized 2B parameter model", "https://huggingface.co/google/gemma-2b-it-cpu-int4/resolve/main/gemma-2b-it-cpu-int4.bin?download=true", "gemma-2b-it-cpu-int4.bin"),
        ModelInfo("MobileBERT Embedding", "Text embedding model for RAG", "https://storage.googleapis.com/download.tensorflow.org/models/tflite/mobilebert_v2.tflite", "mobilebert_v2.tflite")
    ),
    val downloadedModels: List<String> = emptyList(),
    val downloadProgress: Map<String, Int> = emptyMap(),
    val downloadErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val modelManager: ModelManager,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = authRepository.authToken
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        refreshDownloadedModels()
        observeWorkManager()
    }

    private fun observeWorkManager() {
        val workManager = WorkManager.getInstance(context)
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("model_download").collect { workInfos ->
                val progressMap = mutableMapOf<String, Int>()
                val errorMap = mutableMapOf<String, String>()
                for (info in workInfos) {
                    val fileName = info.progress.getString("fileName") ?: info.outputData.getString("fileName")
                    
                    if (!info.state.isFinished) {
                        val progress = info.progress.getInt("progress", 0)
                        if (fileName != null) {
                            progressMap[fileName] = progress
                        }
                    } else if (info.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                        refreshDownloadedModels()
                    } else if (info.state == androidx.work.WorkInfo.State.FAILED) {
                        val errorCode = info.outputData.getInt("error_code", 0)
                        if (fileName != null) {
                            errorMap[fileName] = when (errorCode) {
                                403 -> "Permissions/Terms required (Login below)"
                                401 -> "Login required"
                                else -> "Download failed"
                            }
                        }
                    }
                }
                _uiState.update { it.copy(downloadProgress = progressMap, downloadErrors = errorMap) }
            }
        }
    }

    fun getAuthorizationRequest(): AuthorizationRequest {
        return AuthorizationRequest.Builder(
            ProjectConfig.authServiceConfig,
            ProjectConfig.clientId,
            ResponseTypeValues.CODE,
            ProjectConfig.redirectUri.toUri(),
        )
        .setScope("read-repos")
        .build()
    }

    fun handleAuthResult(result: androidx.activity.result.ActivityResult) {
        val dataIntent = result.data ?: return
        val response = AuthorizationResponse.fromIntent(dataIntent)
        val exception = AuthorizationException.fromIntent(dataIntent)

        if (response != null) {
            val authService = AuthorizationService(context)
            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenEx ->
                if (tokenResponse != null) {
                    viewModelScope.launch {
                        authRepository.saveToken(
                            AuthToken(
                                accessToken = tokenResponse.accessToken!!,
                                refreshToken = tokenResponse.refreshToken!!,
                                expiresAtMs = tokenResponse.accessTokenExpirationTime!!
                            )
                        )
                    }
                }
                authService.dispose()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.clearToken()
        }
    }

    fun refreshDownloadedModels() {
        val downloaded = modelManager.getLocalModels().map { it.name }
        _uiState.update { it.copy(downloadedModels = downloaded) }
    }

    fun downloadModel(model: ModelInfo) {
        viewModelScope.launch {
            modelManager.downloadModel(model.url, model.fileName)
        }
    }
}


