package com.hassn.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class MonitorMode {
    ALWAYS,
    PRIVATE_ONLY
}

@Serializable
data class MonitoredApp(
    val packageName: String,
    val appName: String,
    val mode: MonitorMode = MonitorMode.ALWAYS,
    val customKeywords: List<String> = emptyList(),
    val enabled: Boolean = true,
    val addedTimestamp: Long = System.currentTimeMillis()
)
