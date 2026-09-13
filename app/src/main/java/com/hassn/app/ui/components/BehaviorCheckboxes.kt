package com.hassn.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hassn.app.R

@Composable
fun BehaviorCheckboxes(
    selected: Set<String>,
    onChange: (Set<String>) -> Unit
) {
    Column {
        BehaviorCheckboxRow(selected, "show_message", "📢", R.string.show_message, onChange)
        BehaviorCheckboxRow(selected, "show_challenge", "🎯", R.string.show_challenge, onChange)
        BehaviorCheckboxRow(selected, "redirect", "🔄", R.string.redirect_app, onChange)
    }
}

@Composable
private fun BehaviorCheckboxRow(
    selected: Set<String>,
    id: String,
    icon: String,
    labelRes: Int,
    onChange: (Set<String>) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = id in selected,
            onCheckedChange = { checked ->
                val next = if (checked) selected + id else selected - id
                // At least one behavior must stay selected
                if (next.isNotEmpty()) onChange(next)
            }
        )
        Text(text = icon, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun BehaviorOrderList(
    order: List<String>,
    onMove: (behavior: String, direction: Int) -> Unit
) {
    Column {
        order.forEachIndexed { index, id ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = behaviorLabel(id), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { onMove(id, -1) },
                    enabled = index > 0
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = stringResource(R.string.move_up)
                    )
                }
                IconButton(
                    onClick = { onMove(id, 1) },
                    enabled = index < order.lastIndex
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = stringResource(R.string.move_down)
                    )
                }
            }
        }
    }
}

@Composable
fun behaviorLabel(id: String): String = stringResource(
    when (id) {
        "show_message" -> R.string.show_message
        "show_challenge" -> R.string.show_challenge
        else -> R.string.redirect_app
    }
)
