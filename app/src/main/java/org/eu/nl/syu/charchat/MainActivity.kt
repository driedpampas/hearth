package org.eu.nl.syu.charchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.ui.screens.ChatScreen
import org.eu.nl.syu.charchat.ui.screens.CreateCharacterScreen
import org.eu.nl.syu.charchat.ui.screens.HomeScreen
import org.eu.nl.syu.charchat.ui.screens.SettingsMainScreen
import org.eu.nl.syu.charchat.ui.screens.SettingsGeneralScreen
import org.eu.nl.syu.charchat.ui.screens.SettingsModelsScreen
import org.eu.nl.syu.charchat.ui.screens.SettingsLiteRtModelsScreen
import org.eu.nl.syu.charchat.ui.screens.SettingsEmbeddingModelsScreen
import org.eu.nl.syu.charchat.ui.screens.SettingsHuggingFaceAccountScreen
import org.eu.nl.syu.charchat.ui.theme.ChatTheme

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.eu.nl.syu.charchat.data.AuthRepository
import org.eu.nl.syu.charchat.data.AuthError
import javax.inject.Inject

import dagger.hilt.android.AndroidEntryPoint

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

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToChat = { id -> navController.navigate("chat/$id") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToCreateCharacter = { navController.navigate("create_character") }
            )
        }
        composable("settings") {
            SettingsMainScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGeneral = { navController.navigate("settings/general") },
                onNavigateToModels = { navController.navigate("settings/models") }
            )
        }
        composable("settings/general") {
            SettingsGeneralScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings/models") {
            SettingsModelsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLiteRt = { navController.navigate("settings/models/litert") },
                onNavigateToEmbeddingModels = { navController.navigate("settings/models/embedding") },
                onNavigateToHuggingFace = { navController.navigate("settings/models/huggingface") }
            )
        }
        composable("settings/models/huggingface") {
            SettingsHuggingFaceAccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings/models/litert") {
            SettingsLiteRtModelsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings/models/embedding") {
            SettingsEmbeddingModelsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("create_character") {
            CreateCharacterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "chat/{characterId}",
            arguments = listOf(navArgument("characterId") { type = NavType.StringType })
        ) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
            ChatScreen(
                characterId = characterId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}