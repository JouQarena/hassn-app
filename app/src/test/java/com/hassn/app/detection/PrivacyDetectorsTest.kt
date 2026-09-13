package com.hassn.app.detection

import android.view.accessibility.AccessibilityNodeInfo
import io.mockk.any
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyDetectorsTest {

    /** Builds a mock AccessibilityNodeInfo tree for detection tests. */
    private class NodeSpec(
        val text: CharSequence? = null,
        val desc: CharSequence? = null,
        val className: CharSequence? = null,
        val viewId: String? = null,
        val children: List<NodeSpec> = emptyList()
    ) {
        fun toMock(): AccessibilityNodeInfo {
            val kids = children.map { it.toMock() }
            val node = mockk<AccessibilityNodeInfo>()
            every { node.childCount } returns kids.size
            every { node.getChild(any()) } answers { kids[it.invocation.args[0] as Int] }
            every { node.text } returns text
            every { node.contentDescription } returns desc
            every { node.className } returns className
            every { node.viewIdResourceName } returns viewId
            return node
        }
    }

    private fun rootOf(vararg specs: NodeSpec): AccessibilityNodeInfo =
        NodeSpec(children = specs.toList()).toMock()

    // --- Chrome ---

    @Test
    fun `chrome detects incognito keyword and icon`() {
        val root = rootOf(
            NodeSpec(text = "You've gone incognito"),
            NodeSpec(desc = "Incognito")
        )
        val detector = ChromePrivacyDetector()
        val result = detector.detect(root)
        assertTrue(result.isPrivate)
        assertTrue(result.score >= detector.threshold)
    }

    @Test
    fun `chrome normal page is not private`() {
        val root = rootOf(NodeSpec(text = "Google"), NodeSpec(text = "About"))
        assertFalse(ChromePrivacyDetector().detect(root).isPrivate)
    }

    // --- Reddit ---

    @Test
    fun `reddit anonymous mode is detected`() {
        val root = rootOf(
            NodeSpec(className = "androidx.appcompat.widget.Toolbar", text = "Anonymous"),
            NodeSpec(text = "Exit anonymous")
        )
        assertTrue(RedditPrivacyDetector().detect(root).isPrivate)
    }

    @Test
    fun `reddit normal feed is not private`() {
        val root = rootOf(
            NodeSpec(className = "androidx.appcompat.widget.Toolbar", text = "Popular"),
            NodeSpec(text = "Some post title")
        )
        assertFalse(RedditPrivacyDetector().detect(root).isPrivate)
    }

    // --- Brave ---

    @Test
    fun `brave private tab is detected`() {
        val root = rootOf(
            NodeSpec(text = "Private Tab"),
            NodeSpec(desc = "private icon")
        )
        assertTrue(BravePrivacyDetector().detect(root).isPrivate)
    }

    // --- Custom ---

    @Test
    fun `custom detector matches user keyword`() {
        val detector = CustomAppPrivacyDetector(keywords = listOf("خاص"))
        val root = rootOf(NodeSpec(text = "وضع خاص"))
        assertTrue(detector.detect(root).isPrivate)
    }

    @Test
    fun `custom detector without keyword is not private`() {
        val detector = CustomAppPrivacyDetector(keywords = listOf("incognito"))
        val root = rootOf(NodeSpec(text = "Hello"))
        assertFalse(detector.detect(root).isPrivate)
    }

    // --- Engine ---

    @Test
    fun `engine returns not private for unknown package`() {
        val engine = PrivacyDetectionEngine()
        val root = rootOf(NodeSpec(text = "anything"))
        assertFalse(engine.detect("com.unknown.app", root).isPrivate)
    }

    @Test
    fun `engine registers and uses custom detector`() {
        val engine = PrivacyDetectionEngine()
        engine.registerCustomDetector("com.custom.app", listOf("مجهول"))
        val root = rootOf(NodeSpec(text = "تصفح مجهول"))
        assertTrue(engine.detect("com.custom.app", root).isPrivate)
    }

    @Test
    fun `engine exposes built-in detectors`() {
        val engine = PrivacyDetectionEngine()
        assertTrue(engine.hasBuiltInDetector("com.reddit.android"))
        assertTrue(engine.hasBuiltInDetector("com.android.chrome"))
        assertTrue(engine.hasBuiltInDetector("com.brave.browser"))
        assertTrue(engine.hasBuiltInDetector("com.chrome.beta"))
        assertFalse(engine.hasBuiltInDetector("com.whatsapp"))
    }
}
