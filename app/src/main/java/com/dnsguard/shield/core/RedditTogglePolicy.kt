package com.dnsguard.shield.core

/** Pure label/check-state rules used by the Reddit accessibility tree walker. */
object RedditTogglePolicy {
    val KEYWORDS: List<String> = listOf(
        "NSFW", "18+", "Mature", "Over 18", "محتوى للبالغين", "إظهار محتوى"
    )

    /** UI labels may be longer than the keyword (e.g. "Show NSFW content").
     * Exact package/window scoping and a nearby checkable control are both
     * required separately by the service, to avoid acting on Reddit posts. */
    fun matchesLabel(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val normalized = text.lowercase().replace(Regex("\\s+"), " ")
        return KEYWORDS.any { normalized.contains(it.lowercase()) }
    }

    /** Never click an OFF switch: ACTION_CLICK on an unchecked node enables it. */
    fun shouldTurnOff(checkable: Boolean, checked: Boolean): Boolean = checkable && checked
}
