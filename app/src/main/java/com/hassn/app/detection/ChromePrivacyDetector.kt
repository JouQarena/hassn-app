package com.hassn.app.detection

import android.view.accessibility.AccessibilityNodeInfo
import com.hassn.app.util.findAllTextNodes
import com.hassn.app.util.findByContentDescription
import com.hassn.app.util.findByViewIdPattern

class ChromePrivacyDetector : PrivacyDetector {
    override val packageName = "com.android.chrome"
    // Higher threshold to avoid false positives from dark mode
    override val threshold = 55

    override fun detect(rootNode: AccessibilityNodeInfo): DetectionResult {
        var score = 0
        val matched = mutableListOf<DetectionSignal>()

        // Signal 1: incognito keyword (strongest, weight 35)
        if (rootNode.findAllTextNodes().any { node ->
                node.text?.contains("incognito", ignoreCase = true) == true ||
                    node.contentDescription?.contains("incognito", ignoreCase = true) == true ||
                    node.text?.contains("خفي") == true
            }) {
            score += 35
            matched.add(DetectionSignal.Keyword(listOf("incognito", "خفي"), 35))
        }

        // Signal 2: incognito icon (weight 25)
        if (rootNode.findByContentDescription("incognito").isNotEmpty() ||
            rootNode.findByViewIdPattern(Regex(".*incognito.*", RegexOption.IGNORE_CASE)).isNotEmpty()
        ) {
            score += 25
            matched.add(DetectionSignal.ResourceId("incognito_icon", 25))
        }

        return DetectionResult(score, threshold, matched)
    }
}
