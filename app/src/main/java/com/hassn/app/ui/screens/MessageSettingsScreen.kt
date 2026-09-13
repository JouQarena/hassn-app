package com.hassn.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hassn.app.R
import com.hassn.app.data.MessageSettings
import com.hassn.app.ui.components.MessageOverlayContent
import com.hassn.app.util.Constants
import com.hassn.app.viewmodel.MessageSettingsViewModel
import kotlinx.coroutines.delay
import java.io.File

private val BG_PALETTE = listOf(
    0xFF2196F3.toInt(), 0xFF4CAF50.toInt(), 0xFFFF9800.toInt(), 0xFF9C27B0.toInt(),
    0xFF212121.toInt(), 0xFF37474F.toInt(), 0xFFE91E63.toInt(), 0xFF607D8B.toInt()
)

private val TEXT_PALETTE = listOf(
    0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFFFEB3B.toInt(), 0xFF90CAF9.toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageSettingsScreen(
    viewModel: MessageSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val saved by viewModel.messageSettings.collectAsState()

    var text by remember { mutableStateOf(saved.text) }
    var bgColor by remember { mutableIntStateOf(saved.backgroundColor) }
    var textColor by remember { mutableIntStateOf(saved.textColor) }
    var opacity by remember { mutableFloatStateOf(saved.backgroundOpacity) }
    var textSize by remember { mutableFloatStateOf(saved.textSize) }
    var duration by remember { mutableIntStateOf(saved.duration) }
    var imagePath by remember { mutableStateOf(saved.imagePath) }

    val draft = remember(text, bgColor, textColor, opacity, textSize, duration, imagePath) {
        MessageSettings(text, imagePath, bgColor, opacity, textColor, textSize, duration)
    }

    val previewVisible by viewModel.previewVisible.collectAsState()

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImagePicked(it, context) { newImage ->
            imagePath = newImage
            viewModel.save(draft.copy(imagePath = newImage))
        } }
    }

    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.customize_message)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cancel))
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            if (it.length <= Constants.MESSAGE_MAX_LENGTH) text = it
                        },
                        label = { Text(stringResource(R.string.message_text)) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${text.length}/${Constants.MESSAGE_MAX_LENGTH}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                item {
                    Text(stringResource(R.string.bg_color), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    ColorPaletteRow(BG_PALETTE, bgColor) { color ->
                        bgColor = color
                        viewModel.save(draft)
                    }
                }

                item {
                    Text(stringResource(R.string.text_color), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    ColorPaletteRow(TEXT_PALETTE, textColor) { color ->
                        textColor = color
                        viewModel.save(draft)
                    }
                }

                item {
                    Text(stringResource(R.string.opacity), style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        onValueChangeFinished = { viewModel.save(draft) },
                        valueRange = 0f..1f
                    )
                }

                item {
                    Text(stringResource(R.string.text_size), style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = textSize,
                        onValueChange = { textSize = it },
                        onValueChangeFinished = { viewModel.save(draft) },
                        valueRange = 12f..32f
                    )
                }

                item {
                    Text(stringResource(R.string.duration), style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = duration.toFloat(),
                        onValueChange = { duration = it.toInt().coerceIn(1, 10) },
                        onValueChangeFinished = { viewModel.save(draft) },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { imageLauncher.launch("image/*") }) {
                            Text(stringResource(R.string.choose_image))
                        }
                        if (imagePath != null) {
                            TextButton(onClick = {
                                imagePath = null
                                viewModel.save(draft)
                            }) {
                                Text(stringResource(R.string.remove_image))
                            }
                        }
                    }
                    imagePath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = {
                            viewModel.save(draft)
                            viewModel.showPreview()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.preview))
                    }
                }
            }
        }

        if (previewVisible) {
            MessagePreviewDialog(settings = draft, onDismiss = { viewModel.hidePreview() })
        }
    }
}

/**
 * Copies a picked image into internal storage (500KB max, jpg/png/webp).
 */
private fun onImagePicked(
    uri: Uri,
    context: android.content.Context,
    onCopied: (String) -> Unit
) {
    val dir = File(context.filesDir, Constants.MESSAGE_IMAGE_DIR).apply { mkdirs() }
    val ext = context.contentResolver.getType(uri)
        ?.substringAfterLast('.', ".jpg")?.lowercase() ?: ".jpg"
    if (ext !in Constants.ALLOWED_IMAGE_FORMATS) return

    val target = File(dir, "message_${System.currentTimeMillis()}$ext")
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
    } catch (e: Exception) {
        return
    }
    if (target.length() > Constants.MAX_MESSAGE_IMAGE_KB * 1024L) {
        target.delete()
        return
    }
    onCopied(target.absolutePath)
}

@Composable
private fun ColorPaletteRow(colors: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        3.dp,
                        if (color == selected) MaterialTheme.colorScheme.onSurface
                        else Color.Transparent,
                        CircleShape
                    )
                    .clickable { onSelect(color) }
            )
        }
    }
}

@Composable
private fun MessagePreviewDialog(settings: MessageSettings, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(settings.duration * 1000L + 2000L)
        onDismiss()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MessageOverlayContent(settings)
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.cancel), color = Color.White)
        }
    }
}
