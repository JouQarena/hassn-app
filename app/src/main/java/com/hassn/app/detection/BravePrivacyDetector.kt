package com.hassn.app.detection

import android.view.accessibility.AccessibilityNodeInfo
import com.hassn.app.util.findAllTextNodes
import com.hassn.app.util.findByContentDescription
import com.hassn.app.util.findByText
import com.hassn.app.util.findByViewIdPattern

class BravePrivacyDetector : PrivacyDetector {
    override val packageName = "com.brave.browser"
    override val threshold = 50

    override fun detect(rootNode: AccessibilityNodeInfo): DetectionResult {
        var score = 0
        val matched = mutableListOf<DetectionSignal>()

        // Signal 1: "Private" keyword in tab title (weight 35)
        if (rootNode.findAllTextNodes().any { node ->
                node.text?.let { it.contains("Private") || it.contains("خاص") } == true ||
                    node.contentDescription?.let { it.contains("Private") || it.contains("خاص") } == true
            }) {
            score += 35
            matched.add(DetectionSignal.Keyword(listOf("Private Tab", "خاص"), 35))
        }

        // Signal 2: private icon (weight 25)
        if (rootNode.findByContentDescription("private").isNotEmpty() ||
            rootNode.findByContentDescription("خاص").isNotEmpty() ||
            rootNode.findByViewIdPattern(Regex(".*private.*", RegexOption.IGNORE_CASE)).isNotEmpty()
        ) {
            score += 25
            matched.add(DetectionSignal.ResourceId("private_icon", 25))
        }

        // Signal 3: "New Private Tab" menu button (weight 15)
        if (rootNode.findByText("New Private Tab").isNotEmpty()) {
            score += 15
            matched.add(DetectionSignal.Keyword(listOf("New Private Tab"), 15))
        }

        return DetectionResult(score, threshold, matched)
    }
}
