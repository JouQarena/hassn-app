package com.dnsguard.shield.util

/**
 * RFC-952 / RFC-1123 style validation for DNS hostnames used with Android's
 * "Private DNS provider hostname" setting.
 *
 * Deliberately dependency-free so it can be unit tested on the JVM.
 */
object DnsHostname {

    private const val MAX_TOTAL_LENGTH = 253
    private const val MAX_LABEL_LENGTH = 63

    /** Allowed: letters, digits, hyphens (not leading/trailing), dots between labels. */
    private val LABEL_REGEX = Regex("^(?!-)[A-Za-z0-9-]{1,63}(?<!-)$")

    /**
     * Returns true when [candidate] is a usable Private DNS hostname.
     *
     * Rules enforced:
     *  - trimmed, non-empty, ≤ 253 characters
     *  - at least two labels separated by dots (e.g. `adguard-dns.com`)
     *  - every label is 1..63 chars, alphanumeric/hyphen, no leading or trailing hyphen
     *  - the final label may not be purely numeric (that would be an IPv4-ish value)
     */
    fun isValid(candidate: String): Boolean {
        val host = candidate.trim()
        if (host.isEmpty() || host.length > MAX_TOTAL_LENGTH) return false
        if (host.startsWith(".") || host.endsWith(".")) return false
        if (host.any { it == ' ' || it == '\t' }) return false

        val labels = host.split('.')
        if (labels.size < 2) return false

        for (label in labels) {
            if (label.length > MAX_LABEL_LENGTH) return false
            if (!LABEL_REGEX.matches(label)) return false
        }

        val tld = labels.last()
        if (tld.all { it.isDigit() }) return false
        return true
    }

    /** Normalises user input: trims whitespace and lowercases. */
    fun normalize(candidate: String): String = candidate.trim().lowercase()
}
