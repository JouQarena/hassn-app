package com.hassn.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.hassn.app.data.SettingsDataStore
import com.hassn.app.util.LocaleHelper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

class HassnApp : Application() {

    lateinit var settingsDataStore: SettingsDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsDataStore = SettingsDataStore(this)
        // Apply saved language on cold start
        try {
            val lang = runBlocking { settingsDataStore.appLanguage.first() }
            if (lang.isNotBlank()) {
                LocaleHelper.applyLanguage(this, lang)
            }
        } catch (_: Exception) {}
        Log.d(TAG, "Hassn Application initialised")
    }

    override fun attachBaseContext(base: Context) {
        // For bilingual support, wrap base context
        // We need DataStore read synchronously - fallback to system if not yet known
        val wrapped = try {
            // Can't use DataStore here reliably (no runBlocking in attachBaseContext ideally)
            // Use prefs directly via SharedPrefs-like fallback - just use system default for first launch
            base
        } catch (_: Exception) { base }
        super.attachBaseContext(wrapped)
    }

    companion object {
        private const val TAG = "HassnApp"
        @Volatile
        lateinit var instance: HassnApp
            private set
    }
}

// Alias للتوافق مع الاسم القديم
typealias FocusRedirectApp = HassnApp
