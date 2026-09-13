package com.hassn.app.detection

data class DetectionResult(
    val score: Int,
    val threshold: Int,
    val matchedSignals: List<DetectionSignal>,
    val isPrivate: Boolean = score >= threshold,
    val metadata: Map<String, Any> = emptyMap()
)
