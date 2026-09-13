package com.hassn.app.detection

import android.view.accessibility.AccessibilityNodeInfo
import com.hassn.app.util.Constants

class PrivacyDetectionEngine {

    private val detectors = mutableMapOf<String, PrivacyDetector>()
    private val cache = mutableMapOf<String, CachedResult>()
    private val lock = Any()

    init {
        detectors["com.reddit.android"] = RedditPrivacyDetector()

        val brave = BravePrivacyDetector()
        listOf(
            "com.brave.browser",
            "com.brave.browser.beta",
            "com.brave.browser.nightly"
        ).forEach { detectors[it] = brave }

        val chrome = ChromePrivacyDetector()
        listOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "org.chromium.chrome"
        ).forEach { detectors[it] = chrome }
    }

    fun detect(packageName: String, rootNode: AccessibilityNodeInfo): DetectionResult {
        val detector = detectors[packageName] ?: return DetectionResult(0, 100, emptyList())

        val now = System.currentTimeMillis()
        synchronized(lock) {
            val cached = cache[packageName]
            if (cached != null && now - cached.timestamp < Constants.DETECTION_CACHE_TTL_MS) {
                return cached.result
            }
        }

        val result = try {
            detector.detect(rootNode)
        } catch (e: Exception) {
            DetectionResult(0, 100, emptyList())
        }

        synchronized(lock) {
            cache[packageName] = CachedResult(result, now)
        }
        return result
    }

    fun registerCustomDetector(packageName: String, keywords: List<String>) {
        synchronized(lock) {
            cache.remove(packageName)
            if (keywords.isNotEmpty()) {
                detectors[packageName] = CustomAppPrivacyDetector(keywords)
            } else {
                detectors.remove(packageName)
            }
        }
    }

    fun hasBuiltInDetector(packageName: String): Boolean = synchronized(lock) {
        detectors.containsKey(packageName)
    }

    data class CachedResult(val result: DetectionResult, val timestamp: Long)
}
