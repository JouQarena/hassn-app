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
            // Android 10: تجنب تماماً lowercase/تركيب الحروف المعقدة - فقط نظف الـ surrogates والتحكم
            val sb = StringBuilder(src.length)
            var i = 0
            while (i < src.length) {
                val cp = src.codePointAt(i)
                if (cp == 0xFFFD || (cp in 0xD800..0xDFFF) || cp < 32) {
                    sb.append(' ')
                } else if (cp in 0x200B..0x200F || cp in 0x202A..0x202E || cp == 0xFEFF) {
                    // إزالة محددات الاتجاه RTL/LTR الخفية التي تسبب كراش في Android 10
                } else {
                    sb.appendCodePoint(cp)
                }
                i += Character.charCount(cp)
            }
            var s = sb.toString().trim().replace(Regex("\\s+"), " ")
            if (s.length > 50) s = s.take(50)
            s
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
