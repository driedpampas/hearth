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
import org.eu.nl.syu.charchat.ui.screens.SettingsScreen
import org.eu.nl.syu.charchat.ui.theme.ChatTheme

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

    // Mock data for initial UI demonstration
    val predefinedCharacters = listOf(
        Character("1", "Eldrin the Wise", null, null, "An ancient wizard with deep knowledge.", "System Lore...", true),
        Character("2", "Kaelen Shadowstep", null, null, "A mysterious rogue with a dark past.", "System Lore...", true),
        Character("3", "Lyra Heartfelt", null, null, "A kind-hearted bard who loves music.", "System Lore...", true),
        Character("4", "Grom Ironfist", null, null, "A fierce warrior who values honor.", "System Lore...", true)
    )

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToChat = { id -> navController.navigate("chat/$id") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToCreateCharacter = { navController.navigate("create_character") },
                predefinedCharacters = predefinedCharacters
            )
        }
        composable("settings") {
            SettingsScreen(
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