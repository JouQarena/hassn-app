package com.hassn.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.repeatOnLifecycle
import com.hassn.app.HassnApp
import com.hassn.app.R
import com.hassn.app.admin.UninstallWarningActivity
import com.hassn.app.ui.challenges.ChallengeActivity
import com.hassn.app.ui.components.BehaviorCheckboxes
import com.hassn.app.ui.components.BehaviorOrderList
import com.hassn.app.ui.components.MonitoredAppItem
import com.hassn.app.util.Constants
import com.hassn.app.util.isAccessibilityServiceEnabled
import com.hassn.app.viewmodel.ChallengeViewModel
import com.hassn.app.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToAddApp: () -> Unit,
    onNavigateToMessageSettings: () -> Unit,
    onNavigateToChallengeSettings: () -> Unit,
    onNavigateToDestinationPicker: () -> Unit
) {
    val context = LocalContext.current
    val protectionEnabled by viewModel.protectionEnabled.collectAsState()
    val selectedBehaviors by viewModel.selectedBehaviors.collectAsState()
    val behaviorOrder by viewModel.behaviorOrder.collectAsState()
    val monitoredApps by viewModel.monitoredApps.collectAsState()
    val redirectSettings by viewModel.redirectSettings.collectAsState()
    val locale by viewModel.locale.collectAsState()

    val challengeViewModel: ChallengeViewModel = remember {
        ChallengeViewModel(context.applicationContext as HassnApp)
    }
    val isLocked by challengeViewModel.isLocked.collectAsState()
    val disableRequest by challengeViewModel.disableRequest.collectAsState()

    // Re-check permission state periodically while the screen is active
    var permTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            permTick++
            while (isActive) {
                delay(3000)
                permTick++
            }
        }
    }
    val accessibilityEnabled = remember(permTick) {
        isAccessibilityServiceEnabled(context, "com.hassn.app.service.HassnAccessibilityService")
    }
    val overlayGranted = remember(permTick) { Settings.canDrawOverlays(context) }

    // Disable-protection challenge flow
    val challengeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val success = result.resultCode == Activity.RESULT_OK &&
            (result.data?.getBooleanExtra(ChallengeActivity.EXTRA_SUCCESS, false) ?: false)
        challengeViewModel.onDisableChallengeResult(success)
    }
    LaunchedEffect(disableRequest) {
        disableRequest?.let { request ->
            challengeLauncher.launch(
                Intent(context, ChallengeActivity::class.java).apply {
                    putExtra(ChallengeActivity.EXTRA_TYPE, request.type.name)
                    putExtra(ChallengeActivity.EXTRA_DIFFICULTY, request.difficulty.name)
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.toggleLocale {
                                (LocalContext.current as? Activity)?.recreate()
                            }
                        }
                    ) {
                        Text(
                            text = if (locale == "ar") "EN" else "ع",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (!accessibilityEnabled) {
                item {
                    PermissionCard(
                        title = stringResource(R.string.permission_accessibility_title),
                        body = stringResource(R.string.permission_accessibility_body),
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                }
            }
            if (!overlayGranted) {
                item {
                    PermissionCard(
                        title = stringResource(R.string.permission_overlay_title),
                        body = stringResource(R.string.permission_overlay_body),
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    )
                }
            }

            // Protection toggle
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.protection),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Switch(
                                checked = protectionEnabled,
                                enabled = !isLocked,
                                onCheckedChange = { newValue ->
                                    if (!newValue && protectionEnabled) {
                                        challengeViewModel.requestDisable()
                                    } else {
                                        viewModel.toggleProtection()
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.protection_subtitle),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (isLocked) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🔒 ${stringResource(R.string.protection_locked)}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Behavior selection
            item {
                Text(
                    text = stringResource(R.string.behavior_when_distraction),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            item {
                BehaviorCheckboxes(
                    selected = selectedBehaviors,
                    onChange = { viewModel.updateBehaviors(it) }
                )
            }

            // Execution order (reorderable)
            if (selectedBehaviors.size > 1) {
                item {
                    Text(
                        text = stringResource(R.string.execution_order),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                item {
                    BehaviorOrderList(
                        order = behaviorOrder.filter { it in selectedBehaviors },
                        onMove = { id, direction -> viewModel.moveBehavior(id, direction) }
                    )
                }
            }

            // Per-behavior settings
            if (Constants.BEHAVIOR_MESSAGE in selectedBehaviors) {
                item {
                    OutlinedButton(
                        onClick = onNavigateToMessageSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.customize_message))
                    }
                }
            }
            if (Constants.BEHAVIOR_CHALLENGE in selectedBehaviors) {
                item {
                    OutlinedButton(
                        onClick = onNavigateToChallengeSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.challenge_settings))
                    }
                }
            }
            if (Constants.BEHAVIOR_REDIRECT in selectedBehaviors) {
                item {
                    OutlinedButton(
                        onClick = onNavigateToDestinationPicker,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (redirectSettings.destinationPackage != null) {
                                stringResource(
                                    R.string.current_destination,
                                    redirectSettings.destinationPackage
                                )
                            } else {
                                stringResource(R.string.choose_destination_app)
                            }
                        )
                    }
                }
            }

            // Monitored apps
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.monitored_apps, monitoredApps.size),
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onNavigateToAddApp) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_app)
                        )
                    }
                }
            }

            items(monitoredApps) { app ->
                MonitoredAppItem(
                    app = app,
                    onToggle = { viewModel.addOrUpdateMonitoredApp(app.copy(enabled = !app.enabled)) },
                    onDelete = { viewModel.removeMonitoredApp(app.packageName) }
                )
            }

            // Advanced settings
            item {
                Text(
                    text = stringResource(R.string.advanced_settings),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            item {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(context, UninstallWarningActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.uninstall_review))
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(title: String, body: String, onClick: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = body, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onClick) {
                Text(stringResource(R.string.permission_activate))
            }
        }
    }
}
