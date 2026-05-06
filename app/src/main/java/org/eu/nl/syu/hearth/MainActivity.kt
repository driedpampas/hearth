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

package org.eu.nl.syu.hearth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
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
import org.eu.nl.syu.hearth.data.AuthRepository
import org.eu.nl.syu.hearth.ui.screens.CharacterPickerScreen
import org.eu.nl.syu.hearth.ui.screens.ChatScreen
import org.eu.nl.syu.hearth.ui.screens.CreateCharacterScreen
import org.eu.nl.syu.hearth.ui.screens.DebugThemeScreen
import org.eu.nl.syu.hearth.ui.screens.HomeScreen
import org.eu.nl.syu.hearth.ui.screens.ModelPickerScreen
import org.eu.nl.syu.hearth.ui.screens.ModelSettingsScreen
import org.eu.nl.syu.hearth.ui.screens.SettingsEmbeddingModelsScreen
import org.eu.nl.syu.hearth.ui.screens.SettingsGeneralScreen
import org.eu.nl.syu.hearth.ui.screens.SettingsHuggingFaceAccountScreen
import org.eu.nl.syu.hearth.ui.screens.SettingsLiteRtModelsScreen
import org.eu.nl.syu.hearth.ui.screens.SettingsMainScreen
import org.eu.nl.syu.hearth.ui.screens.SettingsModelsScreen
import org.eu.nl.syu.hearth.ui.screens.ThreadSettingsScreen
import org.eu.nl.syu.hearth.ui.screens.UserPersonaScreen
import org.eu.nl.syu.hearth.ui.screens.CreatePersonaScreen
import org.eu.nl.syu.hearth.ui.viewmodels.EditScope
import androidx.navigation.compose.currentBackStackEntryAsState
import org.eu.nl.syu.hearth.ui.theme.ChatTheme
import org.eu.nl.syu.hearth.common.crash.GlobalCrashHandler
import org.eu.nl.syu.hearth.common.crash.CrashReportActivity
import android.content.Intent
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.eu.nl.syu.hearth.ui.components.WavyHorizontalDivider
import javax.inject.Inject

private const val USE_CUSTOM_NAV_FADE = true
private const val NAV_FADE_DURATION_MS = 220

private fun navFadeSpec(): FiniteAnimationSpec<Float> = tween(durationMillis = NAV_FADE_DURATION_MS)

private fun navEnterTransition(): EnterTransition? =
    if (USE_CUSTOM_NAV_FADE) fadeIn(animationSpec = navFadeSpec()) else null

private fun navExitTransition(): ExitTransition? =
    if (USE_CUSTOM_NAV_FADE) fadeOut(animationSpec = navFadeSpec()) else null

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val postMortemReport = GlobalCrashHandler.checkPostMortemCrash(this)

        setContent {
            ChatTheme {
                val authError by authRepository.authError.collectAsStateWithLifecycle()
                var showPostMortemDialog by remember { mutableStateOf(postMortemReport != null) }

                HearthApp()

                if (showPostMortemDialog && postMortemReport != null) {
                    AlertDialog(
                        onDismissRequest = { showPostMortemDialog = false },
                        title = { Text("Previous Session Crash Detected") },
                        text = {
                            Text("The application encountered a failure (Native Crash or ANR) during your last session. Would you like to view the details?")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showPostMortemDialog = false
                                val intent = Intent(this@MainActivity, CrashReportActivity::class.java).apply {
                                    putExtra(CrashReportActivity.EXTRA_CRASH_INFO, postMortemReport)
                                }
                                startActivity(intent)
                            }) {
                                Text("View Details")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPostMortemDialog = false }) {
                                Text("Dismiss")
                            }
                        }
                    )
                }
                
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
fun HearthApp() {
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600
    
    val navItems = remember {
        listOf(
            NavItem("home", "Chats", Icons.AutoMirrored.Filled.Message),
            NavItem("character_picker", "Characters", Icons.Default.Person),
            NavItem("user_persona?scope=${EditScope.GLOBAL.name}", "Personas", Icons.Default.Badge)
        )
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    androidx.compose.runtime.DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, arguments ->
            android.util.Log.d("HearthNav", "Navigated to: ${destination.route} with args: $arguments")
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    val safeNavigateBack: () -> Unit = {
        if (navController.currentBackStackEntry?.lifecycle?.currentState == androidx.lifecycle.Lifecycle.State.RESUMED) {
            navController.popBackStack()
        }
    }

    Scaffold(
        bottomBar = {
            val isTopLevelDestination = currentRoute in listOf(
                "home",
                "character_picker",
                "user_persona?threadId={threadId}&characterId={characterId}&scope={scope}"
            )
            if (!isWideScreen && isTopLevelDestination) {
                ShortNavigationBar {
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route || (item.route.startsWith("user_persona") && currentRoute?.startsWith("user_persona") == true)
                        ShortNavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(item.route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                    ShortNavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("settings") },
                        icon = { Icon(Icons.Default.MoreVert, contentDescription = "Settings") },
                        label = { Text("More") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isTopLevelDestination = currentRoute in listOf(
                "home",
                "character_picker",
                "user_persona?threadId={threadId}&characterId={characterId}&scope={scope}"
            )
            if (isWideScreen && isTopLevelDestination) {
                NavigationRail(
                    header = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                                contentDescription = "Hearth",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            WavyHorizontalDivider(
                                modifier = Modifier.width(48.dp).height(12.dp),
                                waveLength = 40f,
                                waveHeight = 10f
                            )
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route || (item.route.startsWith("user_persona") && currentRoute?.startsWith("user_persona") == true)
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(item.route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        selected = false,
                        onClick = { navController.navigate("settings") },
                        icon = { Icon(Icons.Default.MoreVert, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = { navEnterTransition() ?: fadeIn() },
                    exitTransition = { navExitTransition() ?: fadeOut() }
                ) {
                    composable(route = "home") {
                        HomeScreen(
                            onNavigateToChat = { id -> navController.navigate("chat/$id") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToCreateCharacter = { id: String? ->
                                if (id != null) navController.navigate("create_character?characterId=$id")
                                else navController.navigate("create_character")
                            },
                            onNavigateToModelSettings = { navController.navigate("model_settings") },
                            onNavigateToModelPicker = { navController.navigate("model_picker") },
                            onNavigateToCharacterPicker = { navController.navigate("character_picker") },
                            onNavigateToUserPersona = { tid, cid, scope ->
                                navController.navigate("user_persona?threadId=$tid&characterId=$cid&scope=${scope.name}")
                            },
                            onNavigateToCreatePersona = {
                                navController.navigate("create_persona")
                            }
                        )
                    }
                    composable(route = "create_persona") {
                        CreatePersonaScreen(
                            onNavigateBack = { safeNavigateBack() }
                        )
                    }
                    composable(route = "settings") {
                        SettingsMainScreen(
                            onNavigateBack = {
                                if (navController.currentDestination?.route != "home") {
                                    safeNavigateBack()
                                }
                            },
                            onNavigateToGeneral = { navController.navigate("settings/general") },
                            onNavigateToModels = { navController.navigate("settings/models") },
                            onNavigateToDebugTheme = { navController.navigate("settings/debug_theme") }
                        )
                    }
                    composable(route = "settings/debug_theme") {
                        DebugThemeScreen(
                            onNavigateBack = { safeNavigateBack() }
                        )
                    }
                    composable(route = "settings/general") {
                        SettingsGeneralScreen(
                            onNavigateBack = { safeNavigateBack() }
                        )
                    }
                    composable(route = "settings/models") {
                        SettingsModelsScreen(
                            onNavigateBack = { safeNavigateBack() },
                            onNavigateToLiteRt = { navController.navigate("settings/models/litert") },
                            onNavigateToEmbeddingModels = { navController.navigate("settings/models/embedding") },
                            onNavigateToHuggingFace = { navController.navigate("settings/models/huggingface") },
                            onNavigateToModelSettings = { navController.navigate("model_settings") }
                        )
                    }
                    composable(route = "settings/models/huggingface") {
                        SettingsHuggingFaceAccountScreen(
                            onNavigateBack = { safeNavigateBack() }
                        )
                    }
                    composable(route = "settings/models/litert") {
                        SettingsLiteRtModelsScreen(
                            onNavigateBack = { safeNavigateBack() },
                            onNavigateToModelSettings = { navController.navigate("model_settings") }
                        )
                    }
                    composable(route = "settings/models/embedding") {
                        SettingsEmbeddingModelsScreen(
                            onNavigateBack = { safeNavigateBack() }
                        )
                    }
                    composable(
                        route = "create_character?characterId={characterId}",
                        arguments = listOf(navArgument("characterId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
                    ) { backStackEntry ->
                        val characterId = backStackEntry.arguments?.getString("characterId")
                        CreateCharacterScreen(
                            characterId = characterId,
                            onNavigateBack = { safeNavigateBack() }
                        )
                    }
                    composable(route = "chat/{threadId}") { backStackEntry ->
                        val threadId = backStackEntry.arguments?.getString("threadId") ?: ""
                        ChatScreen(
                            threadId = threadId,
                            onNavigateBack = {
                                if (navController.currentDestination?.route != "home") {
                                    safeNavigateBack()
                                }
                            },
                            onNavigateToModelSettings = { characterId ->
                                navController.navigate("model_settings/$characterId")
                            },
                            onNavigateToEditCharacter = { characterId ->
                                navController.navigate("create_character?characterId=$characterId")
                            },
                            onNavigateToThreadSettings = { tid ->
                                navController.navigate("thread_settings/$tid")
                            }
                        )
                    }
                    composable(route = "thread_settings/{threadId}") { backStackEntry ->
                        val threadId = backStackEntry.arguments?.getString("threadId") ?: ""
                        ThreadSettingsScreen(
                            threadId = threadId,
                            onNavigateBack = { safeNavigateBack() },
                            onNavigateToEditCharacter = { characterId ->
                                navController.navigate("create_character?characterId=$characterId")
                            },
                            onNavigateToModelSettings = { characterId ->
                                navController.navigate("model_settings/$characterId")
                            },
                            onNavigateToEditPersona = { tid, cid, scope ->
                                navController.navigate("user_persona?threadId=$tid&characterId=$cid&scope=${scope.name}")
                            },
                            onNavigateToCustomiseCharacter = { tid, cid, scope ->
                                navController.navigate("model_settings/$cid?threadId=$tid&scope=${scope.name}")
                            }
                        )
                    }
                    composable(
                        route = "user_persona?threadId={threadId}&characterId={characterId}&scope={scope}",
                        arguments = listOf(
                            navArgument("threadId") { type = NavType.StringType; nullable = true; defaultValue = null },
                            navArgument("characterId") { type = NavType.StringType; nullable = true; defaultValue = null },
                            navArgument("scope") { type = NavType.StringType; defaultValue = EditScope.GLOBAL.name }
                        )
                    ) { backStackEntry ->
                        val threadId = backStackEntry.arguments?.getString("threadId")
                        val characterId = backStackEntry.arguments?.getString("characterId")
                        val scopeName = backStackEntry.arguments?.getString("scope") ?: EditScope.GLOBAL.name
                        val scope = EditScope.valueOf(scopeName)

                        UserPersonaScreen(
                            threadId = threadId,
                            characterId = characterId,
                            initialScope = scope,
                            onNavigateBack = { safeNavigateBack() },
                            onNavigateToCreatePersona = { navController.navigate("create_persona") }
                        )
                    }
                    composable(
                        route = "model_settings/{characterId}?threadId={threadId}&scope={scope}",
                        arguments = listOf(
                            navArgument("characterId") { type = NavType.StringType },
                            navArgument("threadId") { type = NavType.StringType; nullable = true; defaultValue = null },
                            navArgument("scope") { type = NavType.StringType; defaultValue = EditScope.CHARACTER.name }
                        )
                    ) { backStackEntry ->
                        val characterId = backStackEntry.arguments?.getString("characterId")
                        val threadId = backStackEntry.arguments?.getString("threadId")
                        val scopeName = backStackEntry.arguments?.getString("scope") ?: EditScope.CHARACTER.name
                        val scope = EditScope.valueOf(scopeName)

                        ModelSettingsScreen(
                            characterId = characterId,
                            threadId = threadId,
                            initialScope = scope,
                            onNavigateBack = { safeNavigateBack() }
                        )
                    }
                    composable(route = "model_settings/{characterId}") { backStackEntry ->
                        val characterId = backStackEntry.arguments?.getString("characterId")
                        ModelSettingsScreen(
                            characterId = characterId,
                            onNavigateBack = { safeNavigateBack() }
                        )
                    }
                    composable(route = "model_settings") {
                        ModelSettingsScreen(
                            characterId = null,
                            onNavigateBack = { safeNavigateBack() }
                        )
                    }
                    composable(route = "model_picker") {
                        ModelPickerScreen(
                            onDismiss = { safeNavigateBack() },
                            onOpenSettings = { navController.navigate("settings/models") },
                            onNavigateToModelSettings = { navController.navigate("model_settings") }
                        )
                    }
                    composable(route = "character_picker") {
                        CharacterPickerScreen(
                            onDismiss = { safeNavigateBack() },
                            onCharacterSelected = { characterId ->
                                safeNavigateBack()
                                navController.navigate("chat/$characterId")
                            },
                            onNavigateToCreateCharacter = { id ->
                                if (id != null) navController.navigate("create_character?characterId=$id")
                                else navController.navigate("create_character")
                            }
                        )
                    }
                }
            }
        }
    }
}
