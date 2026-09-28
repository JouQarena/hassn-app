package com.dnsguard.shield.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dnsguard.shield.ui.screens.AdbGuideScreen
import com.dnsguard.shield.ui.screens.DashboardScreen
import com.dnsguard.shield.ui.screens.DnsSettingsScreen

/** Route table for the three screens in the wireframe set. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val DNS_SETTINGS = "dns_settings"
    const val ADB_GUIDE = "adb_guide"
}

/**
 * App navigation graph: Dashboard → DNS Settings / ADB Guide (and back).
 */
@Composable
fun DnsGuardNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenDnsSettings = { navController.navigate(Routes.DNS_SETTINGS) },
                onOpenAdbGuide = { navController.navigate(Routes.ADB_GUIDE) }
            )
        }

        composable(Routes.DNS_SETTINGS) {
            DnsSettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAdbGuide = { navController.navigate(Routes.ADB_GUIDE) }
            )
        }

        composable(Routes.ADB_GUIDE) {
            AdbGuideScreen(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * Returns a counter that increments every time the composable returns to the
 * foreground — used by screens to refresh permission/DNS snapshots on resume.
 */
@Composable
fun rememberResumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return tick
}
