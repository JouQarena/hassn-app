package com.hassn.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hassn.app.ui.HassnNavHost
import com.hassn.app.ui.theme.HassnTheme
import com.hassn.app.viewmodel.MainViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val app = application as HassnApp
        val locale = Locale(app.appLocale)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as HassnApp
        val mainViewModel = MainViewModel(app)
        setContent {
            HassnTheme {
                HassnNavHost(mainViewModel)
            }
        }
    }
}
