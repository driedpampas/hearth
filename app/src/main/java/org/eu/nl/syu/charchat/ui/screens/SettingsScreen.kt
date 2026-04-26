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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import coil.compose.SubcomposeAsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eu.nl.syu.charchat.data.DownloadState
import org.eu.nl.syu.charchat.data.AllowedModel
import org.eu.nl.syu.charchat.data.ModelRepository
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
import android.content.Intent
import android.net.Uri
import org.eu.nl.syu.charchat.data.HuggingFaceApiService
import org.eu.nl.syu.charchat.data.HuggingFaceApiService.AccessResult

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
    onNavigateToLiteRt: () -> Unit,
    onNavigateToEmbeddingModels: () -> Unit,
    onNavigateToHuggingFace: () -> Unit
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
            
            val viewModel: ModelsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            
            ListItem(
                headlineContent = { Text("Embedding Model") },
                supportingContent = { Text(uiState.selectedEmbeddingModel ?: "None selected") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToEmbeddingModels() }
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
                supportingContent = { 
                    if (isLoggedIn) {
                        Text("Logged in${if (uiState.preferredUsername != null) " as ${uiState.preferredUsername}" else ""}")
                    } else {
                        Text("Not logged in")
                    }
                },
                leadingContent = { 
                    if (isLoggedIn && uiState.profilePicture != null) {
                        SubcomposeAsyncImage(
                            model = uiState.profilePicture,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            loading = {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            },
                            error = {
                                Icon(Icons.Default.AccountCircle, contentDescription = null)
                            }
                        )
                    } else {
                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                    }
                },
                trailingContent = {
                    if (isLoggedIn) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
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
                },
                modifier = if (isLoggedIn) Modifier.clickable { onNavigateToHuggingFace() } else Modifier
            )

            if (isLoggedIn) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                ListItem(
                    headlineContent = { Text("Community Models") },
                    supportingContent = { Text("Manage indexed authors") },
                    leadingContent = { Icon(Icons.Default.Groups, contentDescription = null) }
                )

                var showAddDialog by remember { mutableStateOf(false) }
                var newAuthorName by remember { mutableStateOf("") }

                uiState.communityAuthors.ifEmpty { setOf("litert-community") }.forEach { author ->
                    ListItem(
                        headlineContent = { Text(author) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeCommunityAuthor(author) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        },
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                TextButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Author")
                }

                if (showAddDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddDialog = false },
                        title = { Text("Add Community Author") },
                        text = {
                            TextField(
                                value = newAuthorName,
                                onValueChange = { newAuthorName = it },
                                placeholder = { Text("e.g. google") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (newAuthorName.isNotBlank()) {
                                    viewModel.addCommunityAuthor(newAuthorName.trim())
                                    newAuthorName = ""
                                    showAddDialog = false
                                }
                            }) {
                                Text("Add")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
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
                val isDownloaded = viewModel.isDownloaded(model)
                val fileName = viewModel.getDownloadFileName(model)
                val progress = uiState.downloadProgress[fileName]
                val error = uiState.downloadErrors[fileName]
                
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
                            if (error != null && error.contains("Accept Terms")) {
                                val context = LocalContext.current
                                Button(onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/${model.modelId}"))
                                    context.startActivity(intent)
                                }) {
                                    Text("Open HF")
                                }
                            } else {
                                IconButton(onClick = { viewModel.downloadModel(model) }) {
                                    Icon(if (error != null) Icons.Default.Refresh else Icons.Default.Download, contentDescription = "Download")
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

// --- Embedding Models Selection Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsEmbeddingModelsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadedEmbeddingModels = uiState.availableModels.filter { model ->
        model.taskTypes.contains("embedding") && viewModel.isDownloaded(model)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Embedding Model") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (downloadedEmbeddingModels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No embedding models downloaded.\nGo to LiteRT Models to download one.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(downloadedEmbeddingModels) { model ->
                    val isSelected = uiState.selectedEmbeddingModel == model.name
                    ListItem(
                        headlineContent = { Text(model.name) },
                        supportingContent = { Text(model.description) },
                        trailingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectEmbeddingModel(model) }
                            )
                        },
                        modifier = Modifier.clickable { viewModel.selectEmbeddingModel(model) }
                    )
                }
            }
        }
    }
}

// --- HuggingFace Account Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHuggingFaceAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HuggingFace Account") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.profilePicture != null) {
                SubcomposeAsyncImage(
                    model = uiState.profilePicture,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(CircleShape),
                    loading = {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = uiState.userName ?: "Unknown User",
                style = MaterialTheme.typography.headlineMedium
            )
            
            uiState.preferredUsername?.let { username ->
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            uiState.userEmail?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Access Token",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayToken = uiState.accessToken?.let {
                            if (it.length > 8) {
                                "${it.take(4)}...${it.takeLast(4)}"
                            } else {
                                "****"
                            }
                        } ?: "No token found"
                        
                        Text(
                            text = displayToken,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(onClick = {
                            uiState.accessToken?.let {
                                clipboardManager.setText(AnnotatedString(it))
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy token")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { 
                    viewModel.logout()
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
        }
    }
}

// --- ViewModel and Helper Classes ---

data class ModelsUiState(
    val availableModels: List<AllowedModel> = emptyList(),
    val downloadedModels: List<String> = emptyList(),
    val downloadProgress: Map<String, Int> = emptyMap(),
    val downloadErrors: Map<String, String> = emptyMap(),
    val selectedEmbeddingModel: String? = null,
    val userName: String? = null,
    val preferredUsername: String? = null,
    val userEmail: String? = null,
    val accessToken: String? = null,
    val profilePicture: String? = null,
    val communityAuthors: Set<String> = emptySet()
)

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val modelManager: ModelManager,
    private val modelRepository: ModelRepository,
    private val authRepository: AuthRepository,
    private val hfApiService: HuggingFaceApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = authRepository.authToken
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(availableModels = modelRepository.getAvailableModels()) }
            refreshDownloadedModels()
        }
        observeWorkManager()
        observeSelectedEmbeddingModel()
        observeAuthToken()
        observeCommunityAuthors()
    }

    private fun observeCommunityAuthors() {
        viewModelScope.launch {
            modelRepository.communityAuthors.collect { authors ->
                _uiState.update { it.copy(communityAuthors = authors) }
                // Refresh models when authors change
                _uiState.update { it.copy(availableModels = modelRepository.getAvailableModels()) }
            }
        }
    }

    fun addCommunityAuthor(author: String) {
        viewModelScope.launch {
            modelRepository.addCommunityAuthor(author)
        }
    }

    fun removeCommunityAuthor(author: String) {
        viewModelScope.launch {
            modelRepository.removeCommunityAuthor(author)
        }
    }

    private fun observeAuthToken() {
        viewModelScope.launch {
            authRepository.authToken.collect { token ->
                if (token != null) {
                    val userInfo = hfApiService.whoami()
                    _uiState.update { 
                        it.copy(
                            userName = userInfo?.name,
                            preferredUsername = userInfo?.preferredUsername,
                            userEmail = userInfo?.email,
                            accessToken = token.accessToken,
                            profilePicture = userInfo?.picture
                        )
                    }
                } else {
                    _uiState.update { it.copy(
                        userName = null, 
                        preferredUsername = null,
                        userEmail = null, 
                        accessToken = null,
                        profilePicture = null
                    ) }
                }
            }
        }
    }

    private fun observeSelectedEmbeddingModel() {
        viewModelScope.launch {
            modelRepository.selectedEmbeddingModel.collect { name ->
                _uiState.update { it.copy(selectedEmbeddingModel = name) }
            }
        }
    }

    fun selectEmbeddingModel(model: AllowedModel) {
        viewModelScope.launch {
            modelRepository.setSelectedEmbeddingModel(model.name)
        }
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
        
        // Automatic selection logic for embedding models
        val downloadedEmbeddingModels = _uiState.value.availableModels
            .filter { it.taskTypes.contains("embedding") && downloaded.contains(modelRepository.getDownloadFileName(it)) }
        
        if (downloadedEmbeddingModels.size == 1 && _uiState.value.selectedEmbeddingModel == null) {
            selectEmbeddingModel(downloadedEmbeddingModels[0])
        }
    }

    fun downloadModel(model: AllowedModel) {
        viewModelScope.launch {
            val fileName = modelRepository.getDownloadFileName(model)
            
            // Clear previous error
            _uiState.update { 
                val newErrors = it.downloadErrors.toMutableMap()
                newErrors.remove(fileName)
                it.copy(downloadErrors = newErrors) 
            }

            // Check access if it's a gated community model
            if (model.modelId.contains("/")) { // Basic check for HF models
                val token = authRepository.getAccessToken()
                if (token != null) {
                    val author = model.author ?: model.modelId.substringBefore("/")
                    val repoName = model.modelId.substringAfter("/")
                    val access = hfApiService.checkModelAccess(author, repoName)
                    if (access is AccessResult.Forbidden) {
                        _uiState.update { 
                            val newErrors = it.downloadErrors.toMutableMap()
                            newErrors[fileName] = "Gated - Accept Terms"
                            it.copy(downloadErrors = newErrors) 
                        }
                        return@launch
                    } else if (access is AccessResult.Unauthorized) {
                        _uiState.update { 
                            val newErrors = it.downloadErrors.toMutableMap()
                            newErrors[fileName] = "Unauthorized - Please relogin"
                            it.copy(downloadErrors = newErrors) 
                        }
                        return@launch
                    }
                } else {
                    // Try to download without token, it will fail if it's strictly gated,
                    // but we let DownloadWorker handle it. For HF models, if we have no token,
                    // we could prompt for login, but we'll let it proceed for public models.
                }
            }

            val url = modelRepository.getDownloadUrl(model)
            modelManager.downloadModel(url, fileName)
        }
    }

    fun isDownloaded(model: AllowedModel): Boolean {
        val fileName = modelRepository.getDownloadFileName(model)
        return uiState.value.downloadedModels.contains(fileName)
    }

    fun getDownloadFileName(model: AllowedModel): String {
        return modelRepository.getDownloadFileName(model)
    }
}


