package com.hassn.app.detection

import android.view.accessibility.AccessibilityNodeInfo
import com.hassn.app.util.getAllText

class CustomAppPrivacyDetector(private val keywords: List<String>) : PrivacyDetector {
    override val packageName = "custom"
    override val threshold = 50

    override fun detect(rootNode: AccessibilityNodeInfo): DetectionResult {
        var score = 0
        val matched = mutableListOf<DetectionSignal>()

        val allText = rootNode.getAllText().lowercase()
        for (keyword in keywords) {
            if (allText.contains(keyword.lowercase())) {
                score = 50
                matched.add(DetectionSignal.Keyword(listOf(keyword), 50))
                break
            }
        }

        return DetectionResult(score, threshold, matched)
    }
}
