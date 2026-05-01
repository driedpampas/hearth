package org.eu.nl.syu.charchat.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import androidx.work.await
import coil.compose.SubcomposeAsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.ResponseTypeValues
import org.eu.nl.syu.charchat.common.ProjectConfig
import org.eu.nl.syu.charchat.data.AllowedModel
import org.eu.nl.syu.charchat.data.AuthRepository
import org.eu.nl.syu.charchat.data.AuthToken
import org.eu.nl.syu.charchat.data.HuggingFaceApiService
import org.eu.nl.syu.charchat.data.HuggingFaceApiService.AccessResult
import org.eu.nl.syu.charchat.data.ModelManager
import org.eu.nl.syu.charchat.data.ModelRepository
import org.eu.nl.syu.charchat.common.ModelSizeUtils
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.ReportGmailerrorred
import androidx.compose.material.icons.filled.Warning
import java.util.Locale
import javax.inject.Inject

// --- Main Settings Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGeneral: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToDebugTheme: () -> Unit
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

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                Text(
                    "Debug",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Theme Debug") },
                    supportingContent = { Text("Visualize color scheme and variants") },
                    leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToDebugTheme() }
                )
            }
        }
    }
}

// --- General Settings Screen ---

@OptIn(ExperimentalMaterial3Api::class)
 @Composable
 fun SettingsGeneralScreen(
     onNavigateBack: () -> Unit,
     viewModel: SettingsGeneralViewModel = hiltViewModel()
 ) {
     val experimentalNpuEnabled by viewModel.experimentalNpuEnabled.collectAsStateWithLifecycle(initialValue = false)
     val statsForNerdsEnabled by viewModel.statsForNerdsEnabled.collectAsStateWithLifecycle(initialValue = false)
     val autoLoadChatModel by viewModel.autoLoadChatModel.collectAsStateWithLifecycle(initialValue = false)
     var showNpuWarning by remember { mutableStateOf(false) }

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
             
             HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
             
             Text(
                 "Advanced",
                 style = MaterialTheme.typography.labelLarge,
                 modifier = Modifier.padding(16.dp),
                 color = MaterialTheme.colorScheme.primary
             )
             
              ListItem(
                  headlineContent = { Text("Stats for Nerds") },
                  supportingContent = { Text("Show generation speed and timing per message") },
                  trailingContent = {
                      Switch(
                          checked = statsForNerdsEnabled,
                          onCheckedChange = { viewModel.setStatsForNerdsEnabled(it) }
                      )
                  }
              )

              ListItem(
                  headlineContent = { Text("Auto-load chat model") },
                  supportingContent = { Text("Load the selected Assistant model when opening chat") },
                  trailingContent = {
                      Switch(
                          checked = autoLoadChatModel,
                          onCheckedChange = { viewModel.setAutoLoadChatModel(it) }
                      )
                  }
              )

              ListItem(
                  headlineContent = { Text("Enable Experimental NPU Support") },
                 supportingContent = { Text("Allows forced NPU execution for compatible models. May cause instability or OOM crashes.") },
                 trailingContent = { 
                     Switch(
                         checked = experimentalNpuEnabled, 
                         onCheckedChange = { 
                             if (it) showNpuWarning = true 
                             else viewModel.setExperimentalNpuEnabled(false)
                         }
                     ) 
                 }
             )

            if (showNpuWarning) {
                AlertDialog(
                    onDismissRequest = { showNpuWarning = false },
                    title = { Text("Experimental NPU Support") },
                    text = { Text("NPUs often lack the complex operator support required for Large Language Models. Enabling this may cause the app to crash or run out of memory. Proceed with caution.") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.setExperimentalNpuEnabled(true)
                            showNpuWarning = false
                        }) {
                            Text("Enable")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNpuWarning = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@HiltViewModel
class SettingsGeneralViewModel @Inject constructor(
    private val modelRepository: ModelRepository
) : ViewModel() {

    val experimentalNpuEnabled: Flow<Boolean> = modelRepository.experimentalNpuEnabled

    val statsForNerdsEnabled: Flow<Boolean> = modelRepository.statsForNerdsEnabled

    val autoLoadChatModel: Flow<Boolean> = modelRepository.autoLoadChatModel

    fun setExperimentalNpuEnabled(enabled: Boolean) {
        viewModelScope.launch {
            modelRepository.setExperimentalNpuEnabled(enabled)
        }
    }

    fun setStatsForNerdsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            modelRepository.setStatsForNerdsEnabled(enabled)
        }
    }

    fun setAutoLoadChatModel(enabled: Boolean) {
        viewModelScope.launch {
            modelRepository.setAutoLoadChatModel(enabled)
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
    onNavigateToHuggingFace: () -> Unit,
    onNavigateToModelSettings: () -> Unit
) {
    val viewModel: ModelsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleAuthResult(result)
    }
    val context = LocalContext.current

    val downloadedModels = remember(uiState.availableModels, uiState.downloadedModelFiles) {
        uiState.availableModels.filter { model ->
            !model.taskTypes.contains("embedding") && viewModel.isDownloaded(model)
        }
    }

    val downloadedEmbeddingModels = remember(uiState.availableModels, uiState.downloadedModelFiles) {
        uiState.availableModels.filter { model ->
            model.taskTypes.contains("embedding") && viewModel.isDownloaded(model)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadModels()
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newAuthorName by remember { mutableStateOf("") }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                ListItem(
                    headlineContent = { Text("LiteRT Models") },
                    supportingContent = { Text("Optimized for Android") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToLiteRt() }
                )
            }

            item {
                Text(
                    text = "Installed LiteRT Models",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (downloadedModels.isEmpty()) {
                item {
                    Text(
                        text = "No LiteRT models downloaded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(downloadedModels) { model ->
                    ModelListItem(
                        model = model,
                        isDownloaded = true,
                        progress = null,
                        error = null,
                        onDownload = {},
                        onDelete = { viewModel.deleteModel(model) },
                        onOpenSettings = onNavigateToModelSettings,
                        viewModel = viewModel
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                ListItem(
                    headlineContent = { Text("Embedding Model") },
                    supportingContent = { Text(uiState.selectedEmbeddingModel ?: "None selected") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToEmbeddingModels() }
                )
            }

            if (downloadedEmbeddingModels.isEmpty()) {
                item {
                    Text(
                        text = "No embedding models downloaded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
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

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                ListItem(
                    headlineContent = { Text("GGUF Models") },
                    supportingContent = { Text("Coming Soon") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.alpha(0.5f)
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
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
                                scope.launch {
                                    val authIntent = withContext(Dispatchers.IO) {
                                        val authRequest = viewModel.getAuthorizationRequest()
                                        val authService = AuthorizationService(context.applicationContext)
                                        try {
                                            authService.getAuthorizationRequestIntent(authRequest)
                                        } finally {
                                            authService.dispose()
                                        }
                                    }
                                    launcher.launch(authIntent)
                                }
                            }) {
                                Text("Login")
                            }
                        }
                    },
                    modifier = if (isLoggedIn) Modifier.clickable { onNavigateToHuggingFace() } else Modifier
                )
            }

            if (isLoggedIn) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    ListItem(
                        headlineContent = { Text("Community Models") },
                        supportingContent = { Text("Manage indexed authors") },
                        leadingContent = { Icon(Icons.Default.Groups, contentDescription = null) }
                    )
                }

                items(uiState.communityAuthors.ifEmpty { setOf("litert-community") }.toList()) { author ->
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

                item {
                    TextButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Author")
                    }
                }
            }
        }
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

// --- LiteRT Models Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLiteRtModelsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToModelSettings: () -> Unit,
    viewModel: ModelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val pullToRefreshState = rememberPullToRefreshState()
    var pendingModel by remember { mutableStateOf<AllowedModel?>(null) }
    var termsModel by remember { mutableStateOf<AllowedModel?>(null) }
    var oomWarningModel by remember { mutableStateOf<AllowedModel?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingModel?.let { viewModel.downloadModel(it) }
        }
        pendingModel = null
    }

    val startDownload: (AllowedModel) -> Unit = { model ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                pendingModel = model
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.downloadModel(model)
            }
        } else {
            viewModel.downloadModel(model)
        }
    }



    val (downloaded, available) = remember(uiState.availableModels, uiState.downloadedModelFiles) {
        uiState.availableModels.partition { viewModel.isDownloaded(it) }
    }

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
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = { viewModel.refreshModels() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        "Pull to sync remote models",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (downloaded.isNotEmpty()) {
                    item {
                        Text(
                            "Downloaded",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(downloaded) { model ->
                        ModelListItem(
                            model = model,
                            isDownloaded = true,
                            progress = null,
                            error = null,
                            onDownload = {},
                            onDelete = { viewModel.deleteModel(model) },
                            onOpenSettings = onNavigateToModelSettings,
                            viewModel = viewModel
                        )
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                }

                if (available.isNotEmpty()) {
                    item {
                        Text(
                            "Available to Download",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    items(available) { model ->
                        val fileName = viewModel.getDownloadFileName(model)
                        val progress = uiState.downloadProgress[fileName]
                        val error = uiState.downloadErrors[fileName]
                        val gated = model.gated || isTermsRequiredError(error)

                        ModelListItem(
                            model = model,
                            isDownloaded = false,
                            progress = progress,
                            error = error,
                            onDownload = {
                                if (gated) {
                                    termsModel = model
                                    return@ModelListItem
                                }
                                
                                val modelSize = ModelSizeUtils.parseModelSize(model.name)
                                val compatibility = ModelSizeUtils.checkCompatibility(context, modelSize)
                                
                                if (compatibility == ModelSizeUtils.Compatibility.TOO_BIG || 
                                    compatibility == ModelSizeUtils.Compatibility.UNKNOWN) {
                                    oomWarningModel = model
                                } else {
                                    startDownload(model)
                                }
                            },
                            onDelete = {},
                            onAcceptTerms = { termsModel = model },
                            showTermsAction = gated,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    if (termsModel != null) {
        AlertDialog(
            onDismissRequest = { termsModel = null },
            title = { Text("Accept model terms") },
            text = { Text("Open the model page to review and accept the terms. Refusing keeps you on this screen.") },
            confirmButton = {
                TextButton(onClick = {
                    val model = termsModel
                    termsModel = null
                    if (model != null) {
                        uriHandler.openUri("https://huggingface.co/${model.modelId}")
                    }
                }) { Text("Open model page") }
            },
            dismissButton = {
                TextButton(onClick = { termsModel = null }) { Text("Refuse") }
            }
        )
    }

    if (oomWarningModel != null) {
        AlertDialog(
            onDismissRequest = { oomWarningModel = null },
            title = { Text("Model may be too large") },
            text = { Text("This model's size could not be determined or it is estimated to require more RAM than your device safely provides. It may cause the app to crash or run extremely slowly. Do you still want to download it?") },
            confirmButton = {
                TextButton(onClick = {
                    oomWarningModel?.let { startDownload(it) }
                    oomWarningModel = null
                }) { Text("Download anyway") }
            },
            dismissButton = {
                TextButton(onClick = { oomWarningModel = null }) { Text("Cancel") }
            }
        )
    }
}

private fun isTermsRequiredError(error: String?): Boolean {
    if (error == null) return false
    return error.contains("Accept Terms", ignoreCase = true) ||
        error.contains("Permissions/Terms required", ignoreCase = true) ||
        error.contains("Terms required", ignoreCase = true)
}

@Composable
fun ModelListItem(
    model: AllowedModel,
    isDownloaded: Boolean,
    progress: DownloadStats?,
    error: String?,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onAcceptTerms: (() -> Unit)? = null,
    showTermsAction: Boolean = false,
    viewModel: ModelsViewModel
) {
    val stats = progress
    ListItem(
        headlineContent = { Text(model.name) },
        supportingContent = {
            Column {
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else if (stats != null && stats.progress < 100) {
                    // Hidden description during download
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { stats.progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val downloadedMb = String.format(Locale.US, "%.1f", stats.downloadedBytes / (1024f * 1024f))
                    val totalMb = String.format(Locale.US, "%.1f", stats.totalBytes / (1024f * 1024f))
                    val speedKb = stats.speed / 1024
                    val speedStr = if (speedKb > 1024) "${String.format(Locale.US, "%.1f", speedKb / 1024f)} MB/s" else "$speedKb KB/s"
                    val etaStr = if (stats.eta > 0) {
                        if (stats.eta > 60) "${stats.eta / 60}m ${stats.eta % 60}s" else "${stats.eta}s"
                    } else ""
                    
                    Text(
                        text = "$downloadedMb MB / $totalMb MB • $speedStr" + if (etaStr.isNotEmpty()) " • $etaStr left" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val author = model.author ?: "Unknown Author"
                    val modelSize = ModelSizeUtils.parseModelSize(model.name)
                    val context = LocalContext.current
                    val compatibility = ModelSizeUtils.checkCompatibility(context, modelSize)

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.alpha(0.8f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            val sizeText = modelSize?.let { ModelSizeUtils.formatParameterCount(it) } ?: "?"
                            Text(
                                text = "$sizeText Parameters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            val (color, text, icon) = when (compatibility) {
                                ModelSizeUtils.Compatibility.FITS -> Triple(Color(0xFF4CAF50), "Fits", Icons.Default.CheckCircle)
                                ModelSizeUtils.Compatibility.CLOSE -> Triple(Color(0xFFFF9800), "Cutting it close", Icons.Default.Warning)
                                ModelSizeUtils.Compatibility.TOO_BIG -> Triple(Color(0xFFF44336), "Too big", Icons.Default.Report)
                                ModelSizeUtils.Compatibility.UNKNOWN -> Triple(Color.Gray, "Model may not fit", Icons.Default.Warning)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = color
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = color
                                )
                            }
                        }
                        
//                        Text(
//                            text = model.description,
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.outline,
//                            maxLines = 1,
//                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
//                            modifier = Modifier.padding(top = 2.dp)
//                        )
                    }
                }
            }
        },
        leadingContent = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.ModelTraining,
                    contentDescription = null,
                    tint = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stats != null && stats.progress < 100) {
                    IconButton(onClick = { viewModel.cancelDownload(model) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Cancel Download", tint = MaterialTheme.colorScheme.error)
                    }
                }

                if (isDownloaded) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                    if (onOpenSettings != null) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else if (progress != null && progress.progress < 100) {
                    //Text("${progress.progress}%", style = MaterialTheme.typography.bodySmall)
                } else {
                    if (showTermsAction || isTermsRequiredError(error)) {
                        Button(onClick = onAcceptTerms ?: onDownload) {
                            Text(if (showTermsAction && error == null) "Open terms" else "Terms")
                        }
                    } else {
                        IconButton(onClick = onDownload) {
                            Icon(
                                if (error != null) Icons.Default.Refresh else Icons.Default.Download,
                                contentDescription = "Download"
                            )
                        }
                    }
                }
            }
        }
    )
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
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.fetchUserInfo()
    }

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
                            uiState.accessToken?.let { token ->
                                scope.launch {
                                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Access Token", token)))
                                }
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

data class DownloadStats(
    val progress: Float,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val speed: Long = 0,
    val eta: Long = -1
)

data class ModelsUiState(
    val availableModels: List<AllowedModel> = emptyList(),
    val downloadProgress: Map<String, DownloadStats> = emptyMap(),
    val downloadErrors: Map<String, String> = emptyMap(),
    val selectedEmbeddingModel: String? = null,
    val userName: String? = null,
    val preferredUsername: String? = null,
    val userEmail: String? = null,
    val accessToken: String? = null,
    val profilePicture: String? = null,
    val communityAuthors: Set<String> = emptySet(),
    val downloadedModelFiles: Set<String> = emptySet()
)

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val modelManager: ModelManager,
    private val modelRepository: ModelRepository,
    private val authRepository: AuthRepository,
    private val hfApiService: HuggingFaceApiService,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()
    private var modelsLoaded = false

    val experimentalNpuEnabled: Flow<Boolean> = modelRepository.experimentalNpuEnabled

    val statsForNerdsEnabled: Flow<Boolean> = modelRepository.statsForNerdsEnabled

    val autoLoadChatModel: Flow<Boolean> = modelRepository.autoLoadChatModel

    fun setExperimentalNpuEnabled(enabled: Boolean) {
        viewModelScope.launch {
            modelRepository.setExperimentalNpuEnabled(enabled)
        }
    }

    fun setStatsForNerdsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            modelRepository.setStatsForNerdsEnabled(enabled)
        }
    }

    fun setAutoLoadChatModel(enabled: Boolean) {
        viewModelScope.launch {
            modelRepository.setAutoLoadChatModel(enabled)
        }
    }

    val isLoggedIn: StateFlow<Boolean> = authRepository.authToken
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        syncDownloadedModels()
        observeWorkManager()
        observeSelectedEmbeddingModel()
        observeAuthToken()
        observeCommunityAuthors()
    }

    fun loadModels() {
        if (modelsLoaded) return
        modelsLoaded = true
        loadCachedModels()
    }

    fun refreshModels() {
        if (!modelsLoaded) {
            modelsLoaded = true
        }
        syncRemoteModels()
    }

    private fun loadCachedModels() {
        viewModelScope.launch {
            val availableModels = withContext(Dispatchers.IO) {
                modelRepository.getAvailableModels()
            }
            _uiState.update { it.copy(availableModels = availableModels) }
        }
    }

    private fun syncRemoteModels() {
        viewModelScope.launch {
            val availableModels = withContext(Dispatchers.IO) {
                modelRepository.refreshAvailableModelsFromRemote()
            }
            _uiState.update { it.copy(availableModels = availableModels) }
        }
    }

    private fun observeCommunityAuthors() {
        viewModelScope.launch {
            modelRepository.communityAuthors.collect { authors ->
                _uiState.update { it.copy(communityAuthors = authors) }
                // Refresh models when authors change
                if (modelsLoaded) {
                    _uiState.update { it.copy(availableModels = modelRepository.getAvailableModels()) }
                }
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

    fun syncDownloadedModels() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                WorkManager.getInstance(context).pruneWork().await()
            }
            refreshDownloadedModels()
        }
    }

    private fun observeAuthToken() {
        viewModelScope.launch {
            authRepository.authToken.collect { token ->
                _uiState.update { 
                    it.copy(
                        accessToken = token?.accessToken,
                    )
                }
            }
        }
    }

    fun fetchUserInfo() {
        viewModelScope.launch {
            val token = authRepository.authToken.first()
            if (token != null) {
                val userInfo = hfApiService.whoami()
                _uiState.update { 
                    it.copy(
                        userName = userInfo?.name,
                        preferredUsername = userInfo?.preferredUsername,
                        userEmail = userInfo?.email,
                        profilePicture = userInfo?.picture
                    )
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
                val progressMap = mutableMapOf<String, DownloadStats>()
                val errorMap = mutableMapOf<String, String>()
                val activeFileNames = mutableSetOf<String>()

                fun resolveFileName(info: androidx.work.WorkInfo): String? {
                    return info.progress.getString("fileName") ?: info.outputData.getString("fileName")
                }

                fun isActive(info: androidx.work.WorkInfo): Boolean {
                    return !info.state.isFinished
                }

                fun downloadErrorMessage(info: androidx.work.WorkInfo): String {
                    val errorCode = info.outputData.getInt("error_code", 0)
                    return when (errorCode) {
                        403 -> "Permissions/Terms required"
                        401 -> "Login required"
                        else -> "Download failed"
                    }
                }

                for (info in workInfos) {
                    val fileName = resolveFileName(info)
                    if (fileName != null && isActive(info)) {
                        activeFileNames += fileName
                    }
                }

                for (info in workInfos) {
                    val fileName = resolveFileName(info)
                    
                    if (!info.state.isFinished) {
                        val progress = info.progress.getFloat("progress", 0f)
                        val downloadedBytes = info.progress.getLong("downloadedBytes", 0)
                        val totalBytes = info.progress.getLong("totalBytes", 0)
                        val speed = info.progress.getLong("speed", 0)
                        val eta = info.progress.getLong("eta", -1)
                        
                        if (fileName != null) {
                            progressMap[fileName] = DownloadStats(
                                progress = progress,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                speed = speed,
                                eta = eta
                            )
                        }
                    } else if (info.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                        refreshDownloadedModels()
                    } else if (info.state == androidx.work.WorkInfo.State.FAILED) {
                        if (fileName != null && fileName !in activeFileNames) {
                            errorMap[fileName] = downloadErrorMessage(info)
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

        if (exception != null) {
            android.util.Log.e("ModelsViewModel", "OAuth authorization failed: ${exception.error} / ${exception.errorDescription}", exception)
        }

        if (response != null) {
            viewModelScope.launch {
                val authService = AuthorizationService(context.applicationContext)
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
                    } else if (tokenEx != null) {
                        android.util.Log.e("ModelsViewModel", "OAuth token exchange failed: ${tokenEx.error} / ${tokenEx.errorDescription}", tokenEx)
                    }
                    authService.dispose()
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.clearToken()
        }
    }

    fun refreshDownloadedModels() {
        val downloadedFiles = modelManager.getLocalModels().map { it.name }.toSet()
        _uiState.update { it.copy(downloadedModelFiles = downloadedFiles) }
        
        // Automatic selection logic for embedding models
        val downloadedEmbeddingModels = _uiState.value.availableModels
            .filter { it.taskTypes.contains("embedding") && isDownloaded(it) }
        
        if (downloadedEmbeddingModels.size == 1 && _uiState.value.selectedEmbeddingModel == null) {
            selectEmbeddingModel(downloadedEmbeddingModels[0])
        }
    }

    fun deleteModel(model: AllowedModel) {
        viewModelScope.launch {
            val fileName = modelRepository.getDownloadFileName(model)
            if (modelManager.deleteModel(fileName)) {
                syncDownloadedModels()
            }
        }
    }

    fun isDownloaded(model: AllowedModel): Boolean {
        val fileName = modelRepository.getDownloadFileName(model)
        return _uiState.value.downloadedModelFiles.contains(fileName)
    }

    fun downloadModel(model: AllowedModel) {
        viewModelScope.launch {
            val fileName = modelRepository.getDownloadFileName(model)
            
            // Clear previous error
            _uiState.update { 
                val newErrors = it.downloadErrors.toMutableMap()
                newErrors.remove(fileName)
                val newProgress = it.downloadProgress.toMutableMap()
                newProgress.remove(fileName)
                it.copy(downloadErrors = newErrors, downloadProgress = newProgress) 
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
                            newErrors[fileName] = "Terms required"
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
            modelManager.downloadModel(url, fileName, modelRepository.serializeModel(model))
        }
    }

    fun cancelDownload(model: AllowedModel) {
        val fileName = modelRepository.getDownloadFileName(model)
        modelManager.cancelDownload(fileName)
        
        // Clear progress from UI immediately
        _uiState.update { 
            val newProgress = it.downloadProgress.toMutableMap()
            newProgress.remove(fileName)
            it.copy(downloadProgress = newProgress)
        }
    }


    fun getDownloadFileName(model: AllowedModel): String {
        return modelRepository.getDownloadFileName(model)
    }
}
