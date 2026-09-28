package com.dnsguard.shield.ui.i18n

/**
 * Supported UI languages. The top bar toggle cycles through these in order:
 * EN → AR → EN.
 *
 * [code] is what the "🌐 ?" button displays.
 * [isRtl] drives `LocalLayoutDirection` so Arabic gets a true right-to-left
 * layout (rows, padding, back arrows and scroll alignment all mirror).
 */
enum class AppLanguage(val code: String, val isRtl: Boolean) {
    EN("EN", isRtl = false),
    AR("AR", isRtl = true);

    fun next(): AppLanguage = when (this) {
        EN -> AR
        AR -> EN
    }

    companion object {
        fun fromCode(raw: String?): AppLanguage =
            // Unknown / legacy codes (e.g. "FR" from an older build) fall back
            // to English.
            entries.firstOrNull { it.code == raw } ?: EN
    }
}
