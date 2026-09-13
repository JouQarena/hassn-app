package com.hassn.app.util

object Constants {
    const val APP_PACKAGE = "com.hassn.app"

    const val BEHAVIOR_MESSAGE = "show_message"
    const val BEHAVIOR_CHALLENGE = "show_challenge"
    const val BEHAVIOR_REDIRECT = "redirect"

    val DEFAULT_BEHAVIORS: Set<String> = setOf(BEHAVIOR_REDIRECT)
    val DEFAULT_BEHAVIOR_ORDER: List<String> =
        listOf(BEHAVIOR_MESSAGE, BEHAVIOR_CHALLENGE, BEHAVIOR_REDIRECT)

    const val DEBOUNCE_MS = 2000L
    const val DETECTION_CACHE_TTL_MS = 2000L
    const val CHALLENGE_TIMEOUT_MS = 60_000L

    const val DISABLE_LOCK_MINUTES = 15

    const val MESSAGE_MAX_LENGTH = 500
    const val MAX_MESSAGE_IMAGE_KB = 500
    val ALLOWED_IMAGE_FORMATS = setOf(".jpg", ".jpeg", ".png", ".webp")
    const val MESSAGE_IMAGE_DIR = "message_images"
}
