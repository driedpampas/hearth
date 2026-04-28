package org.eu.nl.syu.charchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.eu.nl.syu.charchat.data.AuthRepository
import org.eu.nl.syu.charchat.ui.screens.ChatScreen
import org.eu.nl.syu.charchat.ui.screens.CreateCharacterScreen
import org.eu.nl.syu.charchat.ui.screens.HomeScreen
import org.eu.nl.syu.charchat.ui.screens.SettingsEmbeddingModelsScreen
import org.eu.nl.syu.charchat.ui.screens.SettingsGeneralScreen
import org.eu.nl.syu.charchat.ui.screens.SettingsHuggingFaceAccountScreen
import org.eu.nl.syu.charchat.ui.screens.SettingsLiteRtModelsScreen
import org.eu.nl.syu.charchat.ui.screens.SettingsMainScreen
import org.eu.nl.syu.charchat.ui.screens.SettingsModelsScreen
import org.eu.nl.syu.charchat.ui.theme.ChatTheme
import javax.inject.Inject

private const val USE_CUSTOM_NAV_FADE = true
private const val NAV_FADE_DURATION_MS = 220

private fun navFadeSpec(): FiniteAnimationSpec<Float> = tween(durationMillis = NAV_FADE_DURATION_MS)

private fun navEnterTransition(): EnterTransition? =
    if (USE_CUSTOM_NAV_FADE) fadeIn(animationSpec = navFadeSpec()) else null

private fun navExitTransition(): ExitTransition? =
    if (USE_CUSTOM_NAV_FADE) fadeOut(animationSpec = navFadeSpec()) else null

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatTheme {
                val authError by authRepository.authError.collectAsStateWithLifecycle()
                
                CharChatApp()
                
                authError?.let { error ->
                    AlertDialog(
                        onDismissRequest = { authRepository.clearAuthError() },
                        title = { Text("Authentication Error") },
                        text = { 
                            Text("Your Hugging Face session has expired and could not be refreshed. Would you like to log out and log in again?")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                lifecycleScope.launch {
                                    authRepository.clearToken()
                                }
                            }) {
                                Text("Logout")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { authRepository.clearAuthError() }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CharChatApp() {
    val navController = rememberNavController()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(navController = navController, startDestination = "home") {
            composable(route = "home") {
                HomeScreen(
                    onNavigateToChat = { id -> navController.navigate("chat/$id") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToCreateCharacter = { navController.navigate("create_character") }
                )
            }
            composable(route = "settings") {
                SettingsMainScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToGeneral = { navController.navigate("settings/general") },
                    onNavigateToModels = { navController.navigate("settings/models") }
                )
            }
            composable(route = "settings/general") {
                SettingsGeneralScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(route = "settings/models") {
                SettingsModelsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLiteRt = { navController.navigate("settings/models/litert") },
                    onNavigateToEmbeddingModels = { navController.navigate("settings/models/embedding") },
                    onNavigateToHuggingFace = { navController.navigate("settings/models/huggingface") }
                )
            }
            composable(route = "settings/models/huggingface") {
                SettingsHuggingFaceAccountScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(route = "settings/models/litert") {
                SettingsLiteRtModelsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(route = "settings/models/embedding") {
                SettingsEmbeddingModelsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(route = "create_character") {
                CreateCharacterScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(route = "chat/{threadId}") { backStackEntry ->
                val threadId = backStackEntry.arguments?.getString("threadId") ?: ""
                ChatScreen(
                    threadId = threadId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
