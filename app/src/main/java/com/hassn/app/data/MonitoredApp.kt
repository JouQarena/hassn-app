package com.hassn.app.data

import com.hassn.app.util.Constants

/**
 * Represents an app the user wants to be redirected FROM.
 * @param packageName e.g. com.instagram.android
 * @param label Human readable name
 * @param mode either MODE_ALWAYS or MODE_PRIVATE_ONLY
 */
data class MonitoredApp(
    val packageName: String,
    val label: String,
    val mode: String = Constants.MODE_ALWAYS
) {
    val isAlways: Boolean get() = mode == Constants.MODE_ALWAYS
    val isPrivateOnly: Boolean get() = mode == Constants.MODE_PRIVATE_ONLY

    fun toJson(): String = "$packageName|$label|$mode"
    companion object {
        fun fromJson(s: String): MonitoredApp? {
            val parts = s.split("|")
            if (parts.size < 2) return null
            val pkg = parts[0].trim()
            val label = parts.getOrNull(1)?.trim() ?: pkg
            val mode = parts.getOrNull(2)?.trim() ?: Constants.MODE_ALWAYS
            if (pkg.isBlank()) return null
            return MonitoredApp(pkg, label, mode)
        }
        fun listToJson(list: List<MonitoredApp>): String =
            list.joinToString(";;") { it.toJson() }

        fun listFromJson(json: String?): List<MonitoredApp> {
            if (json.isNullOrBlank()) return emptyList()
            return json.split(";;").mapNotNull { fromJson(it) }
        }
    }
}
