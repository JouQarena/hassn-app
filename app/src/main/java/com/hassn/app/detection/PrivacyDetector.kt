package com.hassn.app.detection

import android.view.accessibility.AccessibilityNodeInfo

interface PrivacyDetector {
    fun detect(rootNode: AccessibilityNodeInfo): DetectionResult
    val packageName: String
    val threshold: Int
}
