package com.hassn.app.data

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log

/**
 * Lightweight model representing an installed launchable app.
 */
data class AppInfo(
    val packageName: String,
    val label: String
) {
    /**
     * Returns a copy of this [AppInfo] whose label has been scrubbed of
     * any malformed surrogate pairs or codepoints known to crash the
     * Compose text layout on Android 10 when Arabic text is in play.
     *
     * Falls back to the package name if the resulting string is blank.
     */
    fun sanitiseLabel(): AppInfo {
        val safe = try {
            val src = label
            if (src.isEmpty()) return this.copy(label = packageName)
            val sb = StringBuilder(src.length)
            var i = 0
            while (i < src.length) {
                val cp = src.codePointAt(i)
                // Replace the Unicode replacement character and
                // unpaired surrogates — both of which have crashed
                // Android 10 text shapers when coming from per-app
                // translation labels.
                if (cp == 0xFFFD || (cp in 0xD800..0xDFFF)) {
                    sb.append('?')
                } else {
                    sb.appendCodePoint(cp)
                }
                i += Character.charCount(cp)
            }
            sb.toString()
        } catch (_: Throwable) {
            packageName
        }
        val finalLabel = if (safe.isBlank()) packageName else safe
        return if (finalLabel == label) this else this.copy(label = finalLabel)
    }

    companion object {
        private const val TAG = "AppInfo"

        /**
         * Safely creates an [AppInfo] from a [ResolveInfo].
         *
         * Returns null if the package name cannot be determined,
         * avoiding crashes from malformed resolve info entries.
         */
        fun fromResolveInfoSafe(info: ResolveInfo, pm: PackageManager): AppInfo? {
            return try {
                val pkg = info.activityInfo?.packageName ?: return null
                if (pkg.isBlank()) return null

                // loadLabel can throw or return null on some devices
                val lbl = try {
                    info.loadLabel(pm)?.toString()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load label for $pkg, using package name", e)
                    null
                }

                AppInfo(
                    packageName = pkg,
                    label = lbl?.takeIf { it.isNotBlank() } ?: pkg
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create AppInfo from ResolveInfo", e)
                null
            }
        }
    }
}
