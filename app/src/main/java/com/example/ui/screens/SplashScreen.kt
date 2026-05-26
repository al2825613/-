package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun WassoufSplashScreen(
    onSplashFinished: () -> Unit
) {
    // Elegant entrance animation states for a premium, polished feel
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.85f) }

    LaunchedEffect(key1 = true) {
        // Animate elements beautifully on launch
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
        
        // Show splash screen for exactly 1.8 seconds to optimize performance and prevent wait times
        delay(1800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Keeps the background temporarily solid black to prevent gray flickers
            .testTag("splash_screen_root")
    ) {
        // 1. Full-Screen CenterCrop George Wassouf Image
        Image(
            painter = painterResource(id = R.drawable.img_george_wassouf),
            contentDescription = "أبو وديع - جورج وسوف",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = alphaAnim.value)
        )

        // 2. Cinematic Black Radial/Linear Gradient Overlays for maximum aesthetic readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.90f)
                        )
                    )
                )
        )

        // 3. Middle Content Layout (App Icon + Title + Loading)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .scale(scaleAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Spacer to balance the top
            Spacer(modifier = Modifier.height(48.dp))

            // Icon + Title Box
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Circular app icon framed by a glowing luxury golden border
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon),
                        contentDescription = "تطبيق جورج وسوف",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Majestic title in Arabic
                Text(
                    text = "سلطان الطرب",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.styleOfSultanTitle(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Elegant subtitle
                Text(
                    text = "روائع وأغاني أبو وديع",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom loading indicator and copyright notice
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "روائع الفن الأصيل © 2026",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

// Styling helper to fit the Sultan Title perfectly
@Composable
private fun MaterialTheme.styleOfSultanTitle() = MaterialTheme.typography.headlineLarge.copy(
    fontSize = 32.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.5.sp
)
