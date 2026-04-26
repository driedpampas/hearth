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

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatTheme {
                CharChatApp()
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