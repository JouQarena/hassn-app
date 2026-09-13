package com.hassn.app.data

import kotlinx.serialization.Serializable

@Serializable
data class RedirectSettings(
    val destinationPackage: String? = null,
    val delayMs: Long = 350L
)
