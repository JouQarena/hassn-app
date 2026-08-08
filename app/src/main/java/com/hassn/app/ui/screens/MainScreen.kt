package com.hassn.app.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hassn.app.data.AppInfo
import com.hassn.app.data.MonitoredApp
import com.hassn.app.ui.challenge.DeterrentChallengeOverlay
import com.hassn.app.util.Constants
import com.hassn.app.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val monitoringEnabled by viewModel.monitoringEnabled.collectAsState()
    val targetApp by viewModel.targetApp.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val monitoredApps by viewModel.monitoredApps.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    val isArabic = appLanguage == "ar"
    var showDestinationPicker by remember { mutableStateOf(false) }
    var showMonitoredPicker by remember { mutableStateOf(false) }
    var showChallenge by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf<MonitoredApp?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(if (isArabic) "حصّن" else "Hassn", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            val newLang = if (isArabic) "en" else "ar"
                            viewModel.setAppLanguage(newLang)
                            // Recreate activity to apply
                            try { (context as? android.app.Activity)?.recreate() } catch (_:Exception){}
                        }) {
                            Text(if (isArabic) "EN" else "ع", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            try { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_: Exception) {}
                        }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    MonitoringCardAr(
                        enabled = monitoringEnabled,
                        isArabic = isArabic,
                        onToggleOff = { showChallenge = true },
                        onToggleOn = { viewModel.setMonitoringEnabled(true) }
                    )
                }
                item {
                    DestinationCardAr(targetApp = targetApp, isArabic = isArabic) { showDestinationPicker = true }
                }
                item {
                    MonitoredAppsCardAr(
                        monitoredApps = monitoredApps,
                        isArabic = isArabic,
                        onAdd = { showMonitoredPicker = true },
                        onRemove = { viewModel.removeMonitoredApp(it) },
                        onChangeMode = { showModeDialog = it }
                    )
                }
                item { HowItWorksCard(isArabic) }
                item { AccessibilityStatusCardAr(context, isArabic) }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        if (showDestinationPicker) {
            AppPickerOverlaySingle(
                apps = installedApps,
                selectedPackage = targetApp?.packageName,
                isArabic = isArabic,
                onSelect = { app -> showDestinationPicker = false; viewModel.selectTargetApp(app) },
                onDismiss = { showDestinationPicker = false }
            )
        }

        if (showMonitoredPicker) {
            AppPickerOverlayMulti(
                apps = installedApps,
                alreadySelected = monitoredApps.map { it.packageName }.toSet(),
                isArabic = isArabic,
                onAddSelected = { selected ->
                    showMonitoredPicker = false
                    viewModel.addMonitoredApps(selected, Constants.MODE_ALWAYS)
                },
                onDismiss = { showMonitoredPicker = false }
            )
        }

        showModeDialog?.let { app ->
            ModeChooserDialog(
                app = app,
                isArabic = isArabic,
                onChoose = { mode ->
                    viewModel.updateMonitoredAppMode(app.packageName, mode)
                    showModeDialog = null
                },
                onDismiss = { showModeDialog = null }
            )
        }

        if (showChallenge) {
            DeterrentChallengeOverlay(
                onSolved = { showChallenge = false; viewModel.setMonitoringEnabled(false) },
                onDismiss = { showChallenge = false }
            )
        }
    }
}

@Composable
private fun MonitoringCardAr(enabled: Boolean, isArabic: Boolean, onToggleOff: ()->Unit, onToggleOn: ()->Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(if (enabled) if (isArabic) "الحماية مفعّلة" else "Protection ON" else if (isArabic) "الحماية متوقفة" else "Protection OFF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(if (isArabic) "إعادة توجيه تلقائية عند فتح التطبيقات المراقبة" else "Auto-redirect when monitored apps open", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = { want -> if (want) onToggleOn() else onToggleOff() })
        }
    }
}

@Composable
private fun DestinationCardAr(targetApp: AppInfo?, isArabic: Boolean, onClick: ()->Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Apps, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(if (isArabic) "تطبيق الوجهة" else "Destination", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = targetApp?.let { safeLabel(it) } ?: if (isArabic) "اضغط لاختيار التطبيق" else "Tap to choose", style = MaterialTheme.typography.bodyMedium, color = if (targetApp!=null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Text(if (isArabic) "سيتم نقلك إليه بدلاً من التطبيق المشتت" else "You will be redirected here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MonitoredAppsCardAr(monitoredApps: List<MonitoredApp>, isArabic: Boolean, onAdd: ()->Unit, onRemove: (String)->Unit, onChangeMode: (MonitoredApp)->Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (isArabic) "التطبيقات المراقبة" else "Monitored Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(if (isArabic) "اختر أي تطبيقات تريد الحماية منها" else "Choose apps to redirect from", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isArabic) "إضافة" else "Add")
                }
            }
            Spacer(Modifier.height(12.dp))
            if (monitoredApps.isEmpty()) {
                Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(12.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text(if (isArabic) "لم تختر أي تطبيقات بعد" else "No apps selected yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Text(if (isArabic) "اضغط إضافة لاختيار التطبيقات التي تشتتك" else "Tap Add to choose distracting apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    monitoredApps.forEach { app ->
                        MonitoredAppRow(app, isArabic, onRemove = { onRemove(app.packageName) }, onChangeMode = { onChangeMode(app) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitoredAppRow(app: MonitoredApp, isArabic: Boolean, onRemove: ()->Unit, onChangeMode: ()->Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.6f)), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PhoneAndroid, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                AssistChip(onClick = onChangeMode, label = { Text(if (app.isAlways) if (isArabic) "دائماً" else "Always" else if (isArabic) "خاص فقط" else "Private only", style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(if (app.isAlways) Icons.Default.Bolt else Icons.Default.VisibilityOff, null, modifier = Modifier.size(16.dp)) })
            }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun HowItWorksCard(isArabic: Boolean) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.6f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(if (isArabic) "كيف يعمل؟" else "How it works?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(if (isArabic) "عند فتح تطبيق مراقب يتم الضغط على زر الرئيسية ونقلك فوراً للوجهة." else "When a monitored app opens, Hassn presses Home and launches your destination.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun AccessibilityStatusCardAr(context: Context, isArabic: Boolean) {
    val isEnabled = remember { isAccessibilityEnabled(context) }
    Card(Modifier.fillMaxWidth().clickable { try { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_:Exception){} }, colors = CardDefaults.cardColors(containerColor = if (isEnabled) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, null, tint = if (isEnabled) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Text(if (isEnabled) if (isArabic) "خدمة إمكانية الوصول مفعّلة — الحماية تعمل" else "Accessibility enabled — protection active" else if (isArabic) "إمكانية الوصول غير مفعّلة — اضغط هنا" else "Accessibility NOT enabled — tap here", style = MaterialTheme.typography.bodyMedium, color = if (isEnabled) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeChooserDialog(app: MonitoredApp, isArabic: Boolean, onChoose: (String)->Unit, onDismiss: ()->Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "وضع التحويل — ${app.label}" else "Redirect mode — ${app.label}") },
        text = {
            Column {
                Text(if (isArabic) "اختر متى يتم التحويل:" else "Choose when to redirect:")
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth().clickable { onChoose(Constants.MODE_ALWAYS) }, colors = CardDefaults.cardColors(containerColor = if (app.isAlways) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Bolt, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (isArabic) "دائماً" else "Always", fontWeight = FontWeight.Bold) }
                        Text(if (isArabic) "يتم التحويل فور فتح التطبيق" else "Redirect as soon as app opens", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth().clickable { onChoose(Constants.MODE_PRIVATE_ONLY) }, colors = CardDefaults.cardColors(containerColor = if (app.isPrivateOnly) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.VisibilityOff, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (isArabic) "فقط في الوضع الخاص" else "Private only", fontWeight = FontWeight.Bold) }
                        Text(if (isArabic) "يتم التحويل فقط عند اكتشاف كلمات مثل خفي/خاص" else "Only when incognito keywords detected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (isArabic) "إغلاق" else "Close") } },
        dismissButton = {}
    )
}

// ── Single picker ──
@Composable
private fun AppPickerOverlaySingle(apps: List<AppInfo>, selectedPackage: String?, isArabic: Boolean, onSelect: (AppInfo)->Unit, onDismiss: ()->Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = if (query.isBlank()) apps else apps.filter { safeContains(it.label, query) || safeContains(it.packageName, query) }
    val layoutDirection = LocalLayoutDirection.current
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(0.5f)).clickable(MutableInteractionSource(), null, onClick = onDismiss), contentAlignment = Alignment.Center) {
        Surface(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.82f).clip(RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isArabic) "اختر تطبيق الوجهة" else "Choose Destination", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
                }
                OutlinedTextField(value = query, onValueChange = { query = if (it.length>80) it.take(80) else it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(if (isArabic) "ابحث..." else "Search...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
                Spacer(Modifier.height(12.dp))
                when {
                    apps.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    filtered.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text(if (isArabic) "لا توجد نتائج" else "No results", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    else -> LazyColumn(Modifier.weight(1f)) {
                        items(filtered, key={it.packageName}) { app ->
                            val sel = app.packageName==selectedPackage
                            Row(Modifier.fillMaxWidth().clickable{ try{onSelect(app)}catch(_:Throwable){} }.padding(vertical=12.dp, horizontal=4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if(sel) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked, null, tint = if(sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier=Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f), horizontalAlignment = if(layoutDirection==LayoutDirection.Rtl) Alignment.End else Alignment.Start) {
                                    Text(safeLabel(app), style=MaterialTheme.typography.bodyLarge, fontWeight=if(sel) FontWeight.SemiBold else FontWeight.Normal, textDirection= TextDirection.Content, maxLines=2)
                                    Text(app.packageName, style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant, textDirection= TextDirection.Content, maxLines=1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Multi picker ──
@Composable
private fun AppPickerOverlayMulti(apps: List<AppInfo>, alreadySelected: Set<String>, isArabic: Boolean, onAddSelected: (List<AppInfo>)->Unit, onDismiss: ()->Unit) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val filtered = if (query.isBlank()) apps else apps.filter { safeContains(it.label, query) || safeContains(it.packageName, query) }
    val available = filtered.filterNot { alreadySelected.contains(it.packageName) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(0.5f)).clickable(MutableInteractionSource(), null, onClick = onDismiss), contentAlignment = Alignment.Center) {
        Surface(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.85f).clip(RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(if (isArabic) "اختر التطبيقات المراقبة" else "Choose Monitored Apps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(if (isArabic) "يمكنك اختيار أكثر من تطبيق" else "You can select multiple", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
                }
                OutlinedTextField(value = query, onValueChange = { query = if (it.length>80) it.take(80) else it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(if (isArabic) "ابحث..." else "Search...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
                Spacer(Modifier.height(8.dp))
                Text(if (isArabic) "المحدد: ${selected.size}" else "Selected: ${selected.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                when {
                    apps.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    available.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text(if (isArabic) "لا توجد تطبيقات متاحة" else "No available apps", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    else -> LazyColumn(Modifier.weight(1f)) {
                        items(available, key={it.packageName}) { app ->
                            val sel = selected.contains(app.packageName)
                            Row(Modifier.fillMaxWidth().clickable{ selected = if(sel) selected - app.packageName else selected + app.packageName }.padding(vertical=10.dp, horizontal=4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = sel, onCheckedChange = { checked -> selected = if(checked) selected + app.packageName else selected - app.packageName })
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(safeLabel(app), style=MaterialTheme.typography.bodyMedium, fontWeight=if(sel) FontWeight.SemiBold else FontWeight.Normal, textDirection= TextDirection.Content, maxLines=2)
                                    Text(app.packageName, style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant, textDirection= TextDirection.Content, maxLines=1)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    val chosen = apps.filter { selected.contains(it.packageName) }
                    onAddSelected(chosen)
                }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(if (isArabic) "إضافة (${selected.size})" else "Add (${selected.size})") }
            }
        }
    }
}

private fun safeContains(text: String?, query: String?): Boolean {
    if (text.isNullOrEmpty() || query.isNullOrEmpty()) return false
    return try { text.lowercase(Locale.ROOT).contains(query.lowercase(Locale.ROOT)) } catch (_:Throwable){
        try { val tf=StringBuilder(); val qf=StringBuilder(); for(c in text) tf.append(c.lowercaseChar()); for(c in query) qf.append(c.lowercaseChar()); tf.toString().contains(qf.toString()) } catch (_:Throwable){ text.contains(query, ignoreCase=false) }
    }
}
private fun safeLabel(app: AppInfo): String {
    val raw = app.label
    if (raw.isBlank()) return app.packageName
    return try { val sb=StringBuilder(raw.length); var i=0; while(i<raw.length){ val cp=raw.codePointAt(i); if(cp==0xFFFD || cp in 0xD800..0xDFFF) sb.append('?') else sb.appendCodePoint(cp); i+=Character.charCount(cp)}; sb.toString().ifBlank{app.packageName} } catch(_:Throwable){ app.packageName }
}
private fun isAccessibilityEnabled(context: Context): Boolean {
    return try {
        val serviceId = "${context.packageName}/.service.HassnAccessibilityService"
        val s = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        s.split(':').any { it.equals(serviceId, ignoreCase=true) || it.contains("Hassn") || it.contains("FocusAccessibility") }
    } catch (_:Exception){ false }
}
