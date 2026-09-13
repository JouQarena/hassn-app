package com.hassn.app.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityNodeInfo

private const val MAX_TRAVERSAL_DEPTH = 30

/**
 * Safe traversal of the accessibility node tree.
 * Handles stale/recycled nodes without crashing.
 */
fun AccessibilityNodeInfo.traverse(depth: Int = 0, predicate: (AccessibilityNodeInfo) -> Unit) {
    if (depth > MAX_TRAVERSAL_DEPTH) return
    predicate(this)
    val count = try {
        childCount
    } catch (e: Exception) {
        return
    }
    for (i in 0 until count) {
        val child = try {
            getChild(i)
        } catch (e: Exception) {
            null
        } ?: continue
        try {
            child.traverse(depth + 1, predicate)
        } catch (e: Exception) {
            // skip broken subtree
        }
    }
}

fun AccessibilityNodeInfo.findAllTextNodes(): List<AccessibilityNodeInfo> {
    val result = mutableListOf<AccessibilityNodeInfo>()
    traverse { node ->
        try {
            if (node.text != null || node.contentDescription != null) result.add(node)
        } catch (e: Exception) {
        }
    }
    return result
}

fun AccessibilityNodeInfo.findByClassName(className: String): List<AccessibilityNodeInfo> {
    val result = mutableListOf<AccessibilityNodeInfo>()
    traverse { node ->
        try {
            if (node.className?.contains(className) == true) result.add(node)
        } catch (e: Exception) {
        }
    }
    return result
}

fun AccessibilityNodeInfo.findByText(text: String, ignoreCase: Boolean = false): List<AccessibilityNodeInfo> {
    val result = mutableListOf<AccessibilityNodeInfo>()
    traverse { node ->
        try {
            if (node.text?.contains(text, ignoreCase) == true) result.add(node)
        } catch (e: Exception) {
        }
    }
    return result
}

fun AccessibilityNodeInfo.findByContentDescription(
    text: String,
    ignoreCase: Boolean = true
): List<AccessibilityNodeInfo> {
    val result = mutableListOf<AccessibilityNodeInfo>()
    traverse { node ->
        try {
            if (node.contentDescription?.contains(text, ignoreCase) == true) result.add(node)
        } catch (e: Exception) {
        }
    }
    return result
}

fun AccessibilityNodeInfo.findByViewIdPattern(regex: Regex): List<AccessibilityNodeInfo> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()
    val result = mutableListOf<AccessibilityNodeInfo>()
    traverse { node ->
        try {
            val id = node.viewIdResourceName ?: return@traverse
            if (regex.matches(id)) result.add(node)
        } catch (e: Exception) {
        }
    }
    return result
}

fun AccessibilityNodeInfo.getAllText(): String {
    val builder = StringBuilder()
    traverse { node ->
        try {
            node.text?.let { builder.append(it).append(' ') }
            node.contentDescription?.let { builder.append(it).append(' ') }
        } catch (e: Exception) {
        }
    }
    return builder.toString()
}

fun isAccessibilityServiceEnabled(context: Context, serviceClassName: String): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabled)
    val target = context.packageName + "/" + serviceClassName
    while (splitter.hasNext()) {
        if (splitter.next().equals(target, ignoreCase = true)) return true
    }
    return false
}
