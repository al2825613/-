package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainScreen
import com.example.ui.screens.WassoufSplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WassoufViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val viewModel: WassoufViewModel = viewModel()
                    var showSplash by remember { mutableStateOf(true) }

                    // Highly polished animated cross-fade transition from splash to main content
                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 600)
                    ) { isSplashActive ->
                        if (isSplashActive) {
                            WassoufSplashScreen(
                                onSplashFinished = { showSplash = false }
                            )
                        } else {
                            MainScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
