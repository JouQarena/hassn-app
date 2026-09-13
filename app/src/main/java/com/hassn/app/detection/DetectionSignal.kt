package com.hassn.app.detection

sealed class DetectionSignal(open val weight: Int) {
    data class Keyword(val words: List<String>, override val weight: Int = 30) : DetectionSignal(weight)
    data class ResourceId(val id: String, override val weight: Int = 25) : DetectionSignal(weight)
    data class ColorMatch(val color: Int, val tolerance: Float = 0.05f, override val weight: Int = 20) : DetectionSignal(weight)
    data class IconPresent(val description: String, override val weight: Int = 15) : DetectionSignal(weight)
    data class UiPattern(val description: String, override val weight: Int = 10) : DetectionSignal(weight)
}
