package com.hassn.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hassn.app.HassnApp
import com.hassn.app.ui.screens.AddMonitoredAppScreen
import com.hassn.app.ui.screens.ChallengeSettingsScreen
import com.hassn.app.ui.screens.DestinationAppPickerScreen
import com.hassn.app.ui.screens.MainScreen
import com.hassn.app.ui.screens.MessageSettingsScreen
import com.hassn.app.ui.screens.MonitoredAppConfigScreen
import com.hassn.app.viewmodel.MainViewModel
import com.hassn.app.viewmodel.MessageSettingsViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object AddApp : Screen("add_app")
    object ConfigApp : Screen("config_app/{packageName}")
    object MessageSettings : Screen("message_settings")
    object ChallengeSettings : Screen("challenge_settings")
    object DestinationPicker : Screen("destination_picker")
}

@Composable
fun HassnNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToAddApp = { navController.navigate(Screen.AddApp.route) },
                onNavigateToMessageSettings = { navController.navigate(Screen.MessageSettings.route) },
                onNavigateToChallengeSettings = { navController.navigate(Screen.ChallengeSettings.route) },
                onNavigateToDestinationPicker = { navController.navigate(Screen.DestinationPicker.route) }
            )
        }

        composable(Screen.AddApp.route) {
            AddMonitoredAppScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onAppSelected = { packageName -> navController.navigate("config_app/$packageName") }
            )
        }

        composable(Screen.ConfigApp.route) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName")
                ?: return@composable
            MonitoredAppConfigScreen(
                packageName = packageName,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MessageSettings.route) {
            val app = context.applicationContext as HassnApp
            val messageViewModel = remember { MessageSettingsViewModel(app.settingsRepository) }
            MessageSettingsScreen(
                viewModel = messageViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ChallengeSettings.route) {
            ChallengeSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DestinationPicker.route) {
            DestinationAppPickerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
