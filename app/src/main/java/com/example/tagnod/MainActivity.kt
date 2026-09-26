package com.example.tagnod

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tagnod.data.repository.PreferencesRepository
import com.example.tagnod.nfc.NfcWriteHelper
import com.example.tagnod.nfc.NfcWriteManager
import com.example.tagnod.ui.TagNodViewModelFactory
import com.example.tagnod.ui.editor.MacroEditorScreen
import com.example.tagnod.ui.editor.MacroEditorViewModel
import com.example.tagnod.ui.home.HomeScreen
import com.example.tagnod.ui.home.HomeViewModel
import com.example.tagnod.ui.settings.SettingsScreen
import com.example.tagnod.ui.settings.SettingsViewModel
import com.example.tagnod.ui.theme.TagNodTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent != null) {
            NfcWriteManager.handleIntent(applicationContext, intent)
        }

        setContent {
            val context = LocalContext.current.applicationContext
            val prefRepository = PreferencesRepository(context)
            val themeMode by prefRepository.themeModeFlow.collectAsState(initial = "DARK")
            val accentColorName by prefRepository.accentColorFlow.collectAsState(initial = "WHITE")

            TagNodTheme(themeMode = themeMode, accentColorName = accentColorName) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        val homeViewModel: HomeViewModel = viewModel(
                            factory = TagNodViewModelFactory(context)
                        )
                        HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToEditor = { macroId ->
                                navController.navigate("editor/$macroId")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }

                    composable(
                        route = "editor/{macroId}",
                        arguments = listOf(
                            navArgument("macroId") { type = NavType.LongType }
                        )
                    ) { backStackEntry ->
                        val macroId = backStackEntry.arguments?.getLong("macroId") ?: 0L
                        val editorViewModel: MacroEditorViewModel = viewModel(
                            key = "editor_$macroId",
                            factory = TagNodViewModelFactory(context, macroId)
                        )
                        MacroEditorScreen(
                            viewModel = editorViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("settings") {
                        val settingsViewModel: SettingsViewModel = viewModel(
                            factory = TagNodViewModelFactory(context)
                        )
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NfcWriteManager.handleIntent(applicationContext, intent)
    }

    override fun onResume() {
        super.onResume()
        if (NfcWriteManager.pendingTagId != null) {
            NfcWriteHelper.enableForegroundDispatch(this)
        }
    }

    override fun onPause() {
        super.onPause()
        NfcWriteHelper.disableForegroundDispatch(this)
    }
}
