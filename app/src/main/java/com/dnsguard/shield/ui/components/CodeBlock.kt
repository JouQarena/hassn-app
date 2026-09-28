package com.dnsguard.shield.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dnsguard.shield.ui.i18n.Strings
import com.dnsguard.shield.ui.theme.BorderAndInputBg
import com.dnsguard.shield.ui.theme.CodeBlockBg
import com.dnsguard.shield.ui.theme.CodeBlockText
import com.dnsguard.shield.ui.theme.PrimaryBlue
import com.dnsguard.shield.ui.theme.PureWhite
import com.dnsguard.shield.ui.theme.TextSecondary

/**
 * The ADB guide's command block:
 *  - background #0A0A0A
 *  - text       #80CBC4
 *  - a "copy" affordance that confirms with the localized "Copied ✓" label.
 */
@Composable
fun CodeBlock(
    command: String,
    strings: Strings,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(command) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CodeBlockBg)
            .border(BorderStroke(1.dp, BorderAndInputBg), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.commandLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = if (copied) strings.copiedCommand else "📋 ${strings.copyCommand}",
                style = MaterialTheme.typography.labelSmall,
                color = if (copied) PrimaryBlue else PureWhite,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(command))
                        copied = true
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
        Text(
            text = command,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(14.dp),
            color = CodeBlockText,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
