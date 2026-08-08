package com.hassn.app.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    const val AR = "ar"
    const val EN = "en"

    fun wrapContext(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        // also set layout direction
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun applyLanguage(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun getSystemLanguage(): String {
        val sys = Locale.getDefault().language
        return if (sys == AR) AR else EN
    }
}
