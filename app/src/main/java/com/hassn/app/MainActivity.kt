package com.hassn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.hassn.app.ui.screens.MainScreen
import com.hassn.app.ui.theme.HassnTheme
import com.hassn.app.viewmodel.MainViewModel

/**
 * Single-activity entry point for Focus Redirect.
 *
 * Sets up edge-to-edge rendering, creates the [MainViewModel] via the
 * [ViewModelProvider], and hosts the Compose UI inside a Material 3
 * [Surface] themed with [HassnTheme].
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge system bar rendering (Android 12+ style).
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            HassnTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
