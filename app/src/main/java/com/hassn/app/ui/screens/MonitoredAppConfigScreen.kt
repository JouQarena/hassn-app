package com.hassn.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hassn.app.R
import com.hassn.app.data.MonitoredApp
import com.hassn.app.data.MonitorMode
import com.hassn.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoredAppConfigScreen(
    packageName: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val monitoredApps by viewModel.monitoredApps.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val existing = monitoredApps.find { it.packageName == packageName }
    val appName = installedApps.find { it.packageName == packageName }?.name ?: packageName
    val builtIn = remember { viewModel.hasBuiltInDetector(packageName) }

    var mode by remember { mutableStateOf(existing?.mode ?: MonitorMode.ALWAYS) }
    var keywords by remember { mutableStateOf(existing?.customKeywords ?: emptyList()) }
    var keywordInput by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(existing?.enabled ?: true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = packageName, style = MaterialTheme.typography.bodySmall)

            Text(text = stringResource(R.string.monitoring_mode), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = mode == MonitorMode.ALWAYS,
                    onClick = { mode = MonitorMode.ALWAYS }
                )
                Text(stringResource(R.string.mode_always))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = mode == MonitorMode.PRIVATE_ONLY,
                    onClick = { mode = MonitorMode.PRIVATE_ONLY }
                )
                Text(stringResource(R.string.mode_private_only))
            }

            if (mode == MonitorMode.PRIVATE_ONLY && !builtIn) {
                Text(
                    text = stringResource(R.string.custom_keywords),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = keywordInput,
                        onValueChange = { keywordInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.keyword_hint)) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val keyword = keywordInput.trim()
                            if (keyword.isNotEmpty() && keyword !in keywords) {
                                keywords = keywords + keyword
                                keywordInput = ""
                            }
                        }
                    ) {
                        Text(stringResource(R.string.add_keyword))
                    }
                }
                keywords.forEach { keyword ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        AssistChip(onClick = { }, label = { Text(keyword) })
                        IconButton(onClick = { keywords = keywords - keyword }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.delete),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = enabled, onCheckedChange = { enabled = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.enabled))
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.addOrUpdateMonitoredApp(
                        existing?.copy(
                            mode = mode,
                            customKeywords = keywords,
                            enabled = enabled
                        ) ?: MonitoredApp(
                            packageName = packageName,
                            appName = appName,
                            mode = mode,
                            customKeywords = keywords,
                            enabled = enabled
                        )
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
