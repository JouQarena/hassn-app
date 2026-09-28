package com.dnsguard.shield.core

import android.content.Context
import com.dnsguard.shield.ui.i18n.AppLanguage

/**
 * Tiny SharedPreferences wrapper. Values are written synchronously so the
 * accessibility service (which may be killed at any moment) never loses state.
 */
class Prefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("dns_guard_prefs", Context.MODE_PRIVATE)

    var language: AppLanguage
        get() = AppLanguage.fromCode(prefs.getString(KEY_LANGUAGE, AppLanguage.EN.code))
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value.code).apply()
        }

    private companion object {
        const val KEY_LANGUAGE = "app_language"
    }
}
