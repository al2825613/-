package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.example.ui.player.MusicService
import com.example.ui.screens.MainScreen
import com.example.ui.theme.WassoufSongsTheme
import com.example.ui.viewmodel.WassoufViewModel

@OptIn(UnstableApi::class)
class MainActivity : ComponentActivity() {
    
    private val viewModel: WassoufViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        // Start background media service
        try {
            val serviceIntent = Intent(this, MusicService::class.java)
            startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            WassoufSongsTheme {
                MainScreen(viewModel)
            }
        }
    }
}
