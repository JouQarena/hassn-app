package com.dnsguard.shield.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.dnsguard.shield.LocalPinGate
import com.dnsguard.shield.PinGate
import com.dnsguard.shield.core.security.PinVault
import com.dnsguard.shield.ui.screens.DashboardScreen
import com.dnsguard.shield.ui.screens.DnsSettingsScreen
import com.dnsguard.shield.ui.screens.SetupGuideScreen

/** Route table for the three screens in the wireframe set. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val DNS_SETTINGS = "dns_settings"
    const val SETUP_GUIDE = "setup_guide"
}

/**
 * App navigation graph: Dashboard → DNS Settings / Setup Guide (and back).
 */
@Composable
fun DnsGuardNavHost(navController: NavHostController = rememberNavController()) {
    val gate = LocalPinGate.current
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenDnsSettings = {
                    if (PinVault.isPinSet()) {
                        gate.request(PinGate.Purpose.DNS_SETTINGS) {
                            navController.navigate(Routes.DNS_SETTINGS)
                        }
                    } else navController.navigate(Routes.DNS_SETTINGS)
                },
                onOpenSetupGuide = { navController.navigate(Routes.SETUP_GUIDE) }
            )
        }

        composable(Routes.DNS_SETTINGS) {
            if (PinVault.isPinSet() && !gate.dnsUnlocked) {
                // Fail closed on a restored deep navigation stack: an unlock
                // held only in memory cannot survive rotation/process death.
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            } else {
                DnsSettingsScreen(
                    onBack = {
                        gate.clearDnsUnlock()
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Routes.SETUP_GUIDE) {
            SetupGuideScreen(onBack = { navController.popBackStack() })
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
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return tick
}
