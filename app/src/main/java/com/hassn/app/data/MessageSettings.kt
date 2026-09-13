package com.hassn.app.data

import kotlinx.serialization.Serializable

@Serializable
data class MessageSettings(
    val text: String,
    val imagePath: String? = null,
    val backgroundColor: Int = 0xFF2196F3.toInt(),
    val backgroundOpacity: Float = 0.8f,
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val textSize: Float = 18f,
    val duration: Int = 3
) {
    companion object {
        val DEFAULT = MessageSettings(text = "توقف! ركز على هدفك 💪")
    }
}
