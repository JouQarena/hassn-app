package com.hassn.app.detection

import android.view.accessibility.AccessibilityNodeInfo
import com.hassn.app.util.findByClassName
import com.hassn.app.util.findByText
import com.hassn.app.util.findByViewIdPattern

class RedditPrivacyDetector : PrivacyDetector {
    override val packageName = "com.reddit.android"
    override val threshold = 50

    override fun detect(rootNode: AccessibilityNodeInfo): DetectionResult {
        var score = 0
        val matched = mutableListOf<DetectionSignal>()

        // Signal 1: "Anonymous" keyword in toolbar (weight 40)
        val toolbars = rootNode.findByClassName("Toolbar")
        if (toolbars.any {
                it.text?.contains("Anonymous") == true || it.text?.contains("مجهول") == true
            }) {
            score += 40
            matched.add(DetectionSignal.Keyword(listOf("Anonymous", "مجهول"), 40))
        }

        // Signal 2: anonymous profile icon (weight 30)
        if (rootNode.findByViewIdPattern(Regex(".*anonymous.*", RegexOption.IGNORE_CASE)).isNotEmpty()) {
            score += 30
            matched.add(DetectionSignal.ResourceId("anonymous_profile", 30))
        }

        // Signal 3: "Exit anonymous" button (weight 20)
        if (rootNode.findByText("Exit anonymous").isNotEmpty() ||
            rootNode.findByText("إنهاء التصفح المجهول").isNotEmpty()
        ) {
            score += 20
            matched.add(DetectionSignal.Keyword(listOf("Exit anonymous", "إنهاء التصفح المجهول"), 20))
        }

        return DetectionResult(score, threshold, matched)
    }
}
