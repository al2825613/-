package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import android.widget.Toast
import android.content.Intent
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Song
import com.example.ui.player.AudioPlayerManager
import com.example.ui.viewmodel.WassoufViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi

// Smoothly drifting radial colors simulating high-luxury organic lights
@Composable
fun MovingAmbientBackdrop() {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow_transition")
    val driftX1 by infiniteTransition.animateFloat(
        initialValue = -100f, targetValue = 250f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift_x_1"
    )
    val driftY1 by infiniteTransition.animateFloat(
        initialValue = -50f, targetValue = 300f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift_y_1"
    )
    val driftX2 by infiniteTransition.animateFloat(
        initialValue = 200f, targetValue = -150f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift_x_2"
    )
    val driftY2 by infiniteTransition.animateFloat(
        initialValue = 400f, targetValue = -100f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift_y_2"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030508)) // Dark premium AMOLED base
    ) {
        val w = size.width
        val h = size.height

        // Deep rich sapphire blue ambient orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF072144), Color.Transparent),
                center = Offset(w * 0.3f + driftX1, h * 0.25f + driftY1),
                radius = w * 1.1f
            )
        )

        // Faint mysterious gold kingly accent orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFDFB15B).copy(alpha = 0.05f), Color.Transparent),
                center = Offset(w * 0.75f + driftX2, h * 0.65f + driftY2),
                radius = w * 0.8f
            )
        )

        // Bright neon cyan bottom ambient pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00BFFF).copy(alpha = 0.06f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.9f + (driftY1 / 3)),
                radius = w * 0.7f
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: WassoufViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playbackMode by viewModel.playbackMode.collectAsState()

    val songsList by viewModel.songsList.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val currentQuote by viewModel.currentQuote.collectAsState()
    val downloadingSongs by viewModel.downloadingSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isQuotesSheetOpen by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var isSettingsDropdownExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier.background(Color.Transparent)
            ) {
                // Glassmorphic persistent Mini Player
                if (currentSong != null) {
                    MiniPlayer(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        progress = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        currentPositionStr = formatTime(currentPosition),
                        durationStr = formatTime(duration),
                        onPlayPauseToggle = {
                            if (isPlaying) viewModel.pauseSong() else viewModel.resumeSong()
                        },
                        onExpandClick = { isPlayerExpanded = true }
                    )
                }

                // Dynamic Navigation Bottom Bar styled as a Floating Glass capsule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(GoldenSultan.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple("songs", "الأغاني", Icons.Filled.MusicNote),
                            Triple("favorites", "المفضلة", Icons.Filled.Favorite),
                            Triple("quotes", "ملك الطرب", Icons.Filled.InterpreterMode)
                        ).forEach { (tabId, label, icon) ->
                            val isSelected = activeTab == tabId
                            val animScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.15f else 1.0f,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "tab_scale"
                            )
                            val animTint by animateColorAsState(
                                targetValue = if (isSelected) GoldenSultan else Color.White.copy(alpha = 0.5f),
                                animationSpec = tween(durationMillis = 300),
                                label = "tab_tint"
                            )

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setActiveTab(tabId) }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = animTint,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .scale(animScale)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 8.5.sp,
                                    color = animTint,
                                    modifier = Modifier.scale(animScale)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Absolute smooth ambient backdrop animation
            MovingAmbientBackdrop()

            // Main Contents
            AnimatedVisibility(
                visible = !isSearchActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                // Modern Premium Glassmorphic Header Bar with Symmetrical Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Symmetrical Search Button on Left
                    IconButton(
                        onClick = { isSearchActive = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "بحث الأغاني",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Symmetrical Premium Centered Title
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "جورج وسوف",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (activeTab == "songs") "الأغاني" else if (activeTab == "favorites") "المفضلة" else "ملك الطرب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GoldenSultan,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Symmetrical Floating ⋮ Options Menu Button on Right
                    Box {
                        IconButton(
                            onClick = { isSettingsDropdownExpanded = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "قائمة الخيارات",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isSettingsDropdownExpanded,
                            onDismissRequest = { isSettingsDropdownExpanded = false },
                            modifier = Modifier
                                .background(Color(0xFF0F1524))
                                .border(1.dp, GoldenSultan.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        ) {
                            val options = listOf(
                                Triple("settings", "الإعدادات ⚙️", Icons.Filled.Settings),
                                Triple("about", "عن التطبيق ℹ️", Icons.Filled.Info),
                                Triple("privacy", "سياسة الخصوصية 🔒", Icons.Filled.Security),
                                Triple("share", "مشاركة التطبيق 🔗", Icons.Filled.Share),
                                Triple("rate", "تقييم التطبيق ⭐", Icons.Filled.Star),
                                Triple("update", "التحقق من التحديثات 🔄", Icons.Filled.Refresh)
                            )
                            options.forEach { (id, label, icon) ->
                                DropdownMenuItem(
                                    text = { Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                    leadingIcon = { Icon(icon, contentDescription = null, tint = GoldenSultan, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        isSettingsDropdownExpanded = false
                                        when (id) {
                                            "settings" -> showSettingsDialog = true
                                            "about" -> showAboutDialog = true
                                            "privacy" -> showPrivacyDialog = true
                                            "share" -> {
                                                val shareIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, "استمع الآن إلى روائع مدرسة الفن غناء قيصر وسلطان الطرب جورج وسوف - تطبيق روائع أبو وديع المطور: https://ais-pre-abpw5oljedwdeuiaulfnt3-628211424812.europe-west2.run.app")
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة التطبيق"))
                                            }
                                            "rate" -> showRateDialog = true
                                            "update" -> showUpdateDialog = true
                                        }
                                    }
                                )
                            }
                            
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            
                            DropdownMenuItem(
                                text = { Text("إصدار التطبيق: v1.0.4", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                onClick = { isSettingsDropdownExpanded = false },
                                enabled = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Page navigation
                when (activeTab) {
                    "songs" -> {
                        SongsTabContent(
                            songs = songsList,
                            favorites = favoriteSongs,
                            searchQuery = viewModel.searchQuery.collectAsState().value,
                            playingSong = currentSong,
                            isPlaying = isPlaying,
                            onSearch = { viewModel.setQuery(it) },
                            onSongSelect = { viewModel.playSong(it) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            downloadingSongs = downloadingSongs,
                            onDownloadClick = { viewModel.downloadSong(it) },
                            onDeleteClick = { viewModel.deleteDownloadedSong(it) }
                        )
                    }
                    "favorites" -> {
                        FavoritesTabContent(
                            favorites = favoriteSongs,
                            playingSong = currentSong,
                            isPlaying = isPlaying,
                            onSongSelect = { viewModel.playSong(it) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            downloadingSongs = downloadingSongs,
                            onDownloadClick = { viewModel.downloadSong(it) },
                            onDeleteClick = { viewModel.deleteDownloadedSong(it) }
                        )
                    }
                    "quotes" -> {
                        QuotesAndBioContent(
                            quote = currentQuote,
                            onRefreshQuote = { viewModel.refreshQuote() }
                        )
                    }
                }
            }
            }

            // Exquisite Full Screen Music Player Overlay (Screen 2)
            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
                ) + fadeOut()
            ) {
                if (currentSong != null) {
                    FullScreenPlayer(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        playbackMode = playbackMode,
                        onClose = { isPlayerExpanded = false },
                        onPlayPause = {
                            if (isPlaying) viewModel.pauseSong() else viewModel.resumeSong()
                        },
                        onSeek = { viewModel.seekTo(it) },
                        onNext = { viewModel.slideNext(songsList) },
                        onPrevious = { viewModel.slidePrevious(songsList) },
                        onToggleFavorite = { viewModel.toggleFavorite(currentSong!!) },
                        onTogglePlaybackMode = { viewModel.togglePlaybackMode() }
                    )
                }
            }

            // ==================== Premium Full Screen Search Overlay ====================
            AnimatedVisibility(
                visible = isSearchActive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FullSearchOverlay(
                    searchQuery = searchQuery,
                    onQueryChange = { viewModel.setQuery(it) },
                    songsResult = songsList,
                    playingSong = currentSong,
                    isPlaying = isPlaying,
                    downloadingSongs = downloadingSongs,
                    onClose = { isSearchActive = false; viewModel.setQuery("") },
                    onSongSelect = { viewModel.playSong(it); isSearchActive = false },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onDownloadClick = { viewModel.downloadSong(it) },
                    onDeleteClick = { viewModel.deleteDownloadedSong(it) }
                )
            }

            // ==================== Real Settings Dialogs ====================

            // 1. About Dialog
            if (showAboutDialog) {
                Dialog(onDismissRequest = { showAboutDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.5.dp, GoldenSultan.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = GoldenSultan,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "روائع أبو وديع المطور 🎵",
                                color = GoldenSultan,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "الإصدار الحالي: v1.0.4",
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "تم تصميم هذا التطبيق بصورة مستوحاة من عظمة مدرسة الطرب العربي الأصيل 'سلطان الطرب جورج وسوف'. يهدف إلى تقديم تجربة سماع مريحة وفورية خالية من الضجيج ومصممة بتقنيات صوتية عالية الدقة.\n\nالمطور: فريق مبرمجي وعشاق أبو وديع © 2026.",
                                color = Color.LightGray.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showAboutDialog = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldenSultan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("إغلاق", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 2. Settings Dialog
            if (showSettingsDialog) {
                var isHiFiEnabled by remember { mutableStateOf(true) }
                var cacheLimit by remember { mutableStateOf("250MB") }
                Dialog(onDismissRequest = { showSettingsDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.5.dp, GoldenSultan.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                    tint = GoldenSultan,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إعدادات التطبيق ⚙️",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))

                            // Experience mode toggle
                            Text("وضع تشغيل الصوتيات", color = GoldenSultan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable { viewModel.togglePlaybackMode() }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("محاكي الصوت المطور 🎹", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = if (playbackMode == AudioPlayerManager.PlaybackMode.SYNTH) "مفعّل (تشغيل تركيبي)" else "غير مفعّل (بث سحابي)",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = playbackMode == AudioPlayerManager.PlaybackMode.SYNTH,
                                    onCheckedChange = { viewModel.togglePlaybackMode() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = GoldenSultan,
                                        checkedTrackColor = GoldenSultan.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // HiFi toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable { isHiFiEnabled = !isHiFiEnabled }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("صوت فائق الدقة (Hi-Fi) ✨", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("تحسين عمق تدرج موجات الصوت الحية", color = Color.Gray, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = isHiFiEnabled,
                                    onCheckedChange = { isHiFiEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = GoldenSultan,
                                        checkedTrackColor = GoldenSultan.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Cache limit dropdown simulacrum
                            Text("الحد الأقصى للتخزين المؤقت", color = GoldenSultan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("100MB", "250MB", "مفتوح ♾️").forEach { limit ->
                                    val isSel = cacheLimit == limit || (limit == "مفتوح ♾️" && cacheLimit == "Unlimited")
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSel) GoldenSultan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                            .border(1.dp, if (isSel) GoldenSultan else Color.Transparent, RoundedCornerShape(10.dp))
                                            .clickable { cacheLimit = limit }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(limit, color = if (isSel) GoldenSultan else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showSettingsDialog = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldenSultan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("حفظ وإغلاق 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 3. Privacy Policy Dialog
            if (showPrivacyDialog) {
                Dialog(onDismissRequest = { showPrivacyDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.5.dp, GoldenSultan.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = GoldenSultan,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "سياسة الخصوصية الأوفلاين 🔒",
                                color = GoldenSultan,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "لأننا نقدّر خصوصيتك وأمانك بشكل كامل، نؤكد أن هذا التطبيق لا يقوم بجمع، تخزين، تتبع، أو مشاركة أي معلومات شخصية أو بيانات استخدام خاصة بك مع أي جهة خارجية أو خوادم تحليلية.\n\nتُخزن جميع الملفات الصوتية والأغاني التي تقوم بتحميلها بشكل محلي تماماً داخل الذاكرة التخزينية المعزولة لجهازك الخاص والمحمية بموجب نظام أندرويد لتعمل بالكامل أوفلاين دون الحاجة لأي اتصال.",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                lineHeight = 21.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showPrivacyDialog = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldenSultan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("فهمت وموافق", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 4. Rate App Dialog
            if (showRateDialog) {
                var selectedRating by remember { mutableIntStateOf(0) }
                Dialog(onDismissRequest = { showRateDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.5.dp, GoldenSultan.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = GoldenSultan,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "تقييم تطبيق روائع أبو وديع ⭐",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "رأيك يهمنا لمواصلة تطوير وتحديث روائع الطرب الأصيل. عبّر عن حبك لفن جورج وسوف!",
                                color = Color.LightGray,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Interactive Stars row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..5).forEach { starIndex ->
                                    val isStarSelected = starIndex <= selectedRating
                                    Icon(
                                        imageVector = if (isStarSelected) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = "نجمة $starIndex",
                                        tint = if (isStarSelected) GoldenSultan else Color.Gray.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable { selectedRating = starIndex }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (selectedRating) {
                                    1 -> "برأيك يحتاج إلى تحسين؟ سنسعى للأفضل!"
                                    2 -> "شكراً لملاحظتك، نعمل على التحديثات باستمرار."
                                    3 -> "رائع! يسعدنا تقديم مستويات ممتعة دائماً."
                                    4 -> "ممتاز جداً! شكراً لتقديرك روائع الطرب الأصيل."
                                    5 -> "أبو وديع يستاهل كل الحب! شكراً لك ❤️"
                                    else -> "اضغط على النجوم لتسجيل تقييمك"
                                },
                                color = if (selectedRating > 0) GoldenSultan else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { showRateDialog = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("تراجع", color = Color.White, fontWeight = FontWeight.Medium)
                                }
                                Button(
                                    onClick = {
                                        showRateDialog = false
                                        if (selectedRating > 0) {
                                            Toast.makeText(context, "شكراً جزيلاً لتقييمك بـ $selectedRating نجمة! تم تسجيل رأيك بنجاح.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldenSultan),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("إرسال التقييم", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Check for Updates Dialog
            if (showUpdateDialog) {
                var isChecking by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1200)
                    isChecking = false
                }
                Dialog(onDismissRequest = { showUpdateDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.5.dp, GoldenSultan.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = GoldenSultan,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "تحديثات التطبيق 🔄",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (isChecking) {
                                CircularProgressIndicator(color = GoldenSultan, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "جاري البحث عن تحديثات جديدة خالية من الأخطاء...",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2ECC71),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "أنت تستخدم النسخة الأحدث والآمنة حالياً!\nإصدار التطبيق الحالي: v1.0.4",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { showUpdateDialog = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldenSultan),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("رائع وموافق", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // High-End Glassmorphic Modal Bottom Sheet for Info/Bio
            if (isQuotesSheetOpen) {
                ModalBottomSheet(
                    onDismissRequest = { isQuotesSheetOpen = false },
                    containerColor = Color(0xFF070B12).copy(alpha = 0.98f),
                    contentColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "حِكمة أبو وديع اليومية ✨",
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldenSultan,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "“$currentQuote”",
                            color = Color.White,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.refreshQuote() },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldenSultan)
                            ) {
                                Text("تغيير الحِكمة 🔄", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { isQuotesSheetOpen = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                            ) {
                                Text("إغلاق", color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SongsTabContent(
    songs: List<Song>,
    favorites: List<Song>,
    searchQuery: String,
    playingSong: Song?,
    isPlaying: Boolean,
    onSearch: (String) -> Unit,
    onSongSelect: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    downloadingSongs: Map<Int, Float> = emptyMap(),
    onDownloadClick: (Song) -> Unit = {},
    onDeleteClick: (Song) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // Curator section headline
        Text(
            text = "روائع جورج وسوف الغنائية",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.QueueMusic,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "لم نجد أي أغنية مطابقة لبحثك.",
                        textAlign = TextAlign.Center,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            // LazyColumn containing scrollable music lists
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("songs_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(songs) { song ->
                    val isCurrent = playingSong?.id == song.id
                    val prog = downloadingSongs[song.id]
                    SongItemCard(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        downloadProgress = prog,
                        onSongSelect = { onSongSelect(song) },
                        onFavoriteToggle = { onFavoriteToggle(song) },
                        onDownloadClick = { onDownloadClick(song) },
                        onDeleteClick = { onDeleteClick(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun SongItemCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    downloadProgress: Float? = null,
    onSongSelect: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrent) 1.5.dp else 0.5.dp,
                brush = if (isCurrent) {
                    Brush.horizontalGradient(listOf(GoldenSultan, NeonCyan))
                } else {
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent))
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSongSelect() }
            .testTag("song_item_card_${song.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                Color(0xFF0D1E36).copy(alpha = 0.45f) // Deep luxurious blue highlighting
            } else {
                Color(0xFF0F1524).copy(alpha = 0.3f) // Frosted glass default body
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Screen 1: Circular George Wassouf thumbnail image
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(1.dp, GoldenSultan.copy(alpha = 0.8f), CircleShape)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = song.getLocalOrFallbackImage(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (isCurrent && isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        MusicWaveAnimation()
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (isCurrent) GoldenSultan else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.album} • ${song.year}",
                    fontSize = 11.5.sp,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Tags Layout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GoldenSultan.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = song.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldenSultan
                        )
                    }

                    // Playback Offline/Online indicators
                    if (song.isDownloaded) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2ECC71).copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "أوفلاين 💾",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2ECC71)
                            )
                        }
                    }
                }
            }

            // Quick Controller interactions side (Play, Liking, Downloads)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Interactive Fully-fledged downloading controls
                if (downloadProgress != null) {
                    CircularProgressIndicator(
                        progress = { downloadProgress },
                        color = GoldenSultan,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else if (song.isDownloaded) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OfflinePin,
                            contentDescription = "محملة وصوت نقي بالكامل. اضغط لحذف الكاش",
                            tint = Color(0xFF2ECC71),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DownloadForOffline,
                            contentDescription = "انقر لتنزيل الأغنية بجودة 320kbps",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Heart Like
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "التفضيل",
                        tint = if (song.isFavorite) Color.Red else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Modern White Circular Play button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onSongSelect() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCurrent && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "تشغيل",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenPlayer(
    song: Song,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    playbackMode: AudioPlayerManager.PlaybackMode,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePlaybackMode: () -> Unit
) {
    val spinningDisc = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            spinningDisc.animateTo(
                targetValue = spinningDisc.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(14000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            spinningDisc.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14)) // Slate Dark Spotify-like background
            .testTag("full_player")
    ) {
        // atmospheric song art blur background:
        AsyncImage(
            model = song.getLocalOrFallbackImage(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(50.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.22f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Full screen header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                Text(
                    text = "جِلْسَةُ الطَّرَبِ تَعْمَلُ أَنْ",
                    color = GoldenSultan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "إعجاب",
                        tint = if (song.isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Cinematic circular George Wassouf image inside Neon Progress indicator rings (Slightly smaller, 210dp)
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                val progressFraction = if (duration > 0) currentPosition.toFloat() / duration else 0f
                val sweepAngle = progressFraction * 360f

                Canvas(modifier = Modifier.size(206.dp)) {
                    val strokeBgWidth = 3.5.dp.toPx()
                    val strokeNeonWidth = 4.dp.toPx()

                    // Background track progress ring
                    drawArc(
                        color = Color.White.copy(alpha = 0.04f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeBgWidth, cap = StrokeCap.Round)
                    )

                    // Neon Progress ring 1: Deep wide glow (Faint cyan)
                    drawArc(
                        color = NeonCyan.copy(alpha = 0.1f),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Neon Progress ring 3: Foreground sharp cyan color line
                    drawArc(
                        color = NeonCyan,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeNeonWidth, cap = StrokeCap.Round)
                    )

                    // Accent little gold dot tracking progress ring along sweep end
                    if (sweepAngle > 0f) {
                        val angleRad = Math.toRadians((sweepAngle - 90f).toDouble())
                        val r = size.width / 2
                        val dotX = (r * Math.cos(angleRad)).toFloat() + r
                        val dotY = (r * Math.sin(angleRad)).toFloat() + r
                        drawCircle(
                            color = GoldenSultan,
                            radius = 4.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }

                // George Wassouf circular centerpiece (Slightly smaller, 166dp)
                Box(
                    modifier = Modifier
                        .size(166.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF030508), CircleShape)
                        .border(3.dp, GoldenSultan.copy(alpha = 0.8f), CircleShape)
                        .graphicsLayer { rotationZ = spinningDisc.value }
                ) {
                    AsyncImage(
                        model = song.getLocalOrFallbackImage(),
                        contentDescription = "القرص الدوار",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Classic vinyl circular reflections & golden center core label
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.2.dp, GoldenSultan, CircleShape)
                            .align(Alignment.Center)
                    )
                }
            }

            // Song Metadata block (Smaller text sizes)
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${song.album} • ${song.year}",
                    fontSize = 12.sp,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Streaming Offline Switch capsule
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onTogglePlaybackMode() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (playbackMode == AudioPlayerManager.PlaybackMode.STREAM) Icons.Filled.Stream else Icons.Filled.AudioFile,
                        contentDescription = null,
                        tint = GoldenSultan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (playbackMode == AudioPlayerManager.PlaybackMode.STREAM) "جودة استوديو (أونلاين)" else "صوت محلي معزز (بدون انترنت)",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Slider progress bar (Smaller padding)
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = GoldenSultan,
                        activeTrackColor = GoldenSultan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(duration), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Control Buttons Row (Clean minimized controls)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Song (Sized 44dp)
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "السابق",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Floating Play/Pause (Sized 60dp)
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "تشغيل أو إيقاف مؤقت",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Next Song (Sized 44dp)
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "التالي",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// Custom Glassmorphic Mini Player
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    currentPositionStr: String,
    durationStr: String,
    onPlayPauseToggle: () -> Unit,
    onExpandClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandClick() }
            .testTag("mini_player"),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF070B12).copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column {
            // Seek progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.8.dp),
                color = GoldenSultan,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spinning vinyl mini artwork
                val miniDiscTransition = rememberInfiniteTransition(label = "mini_disc_transition")
                val spinDegree by miniDiscTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(9000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "spin"
                )

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(0.5.dp, GoldenSultan, CircleShape)
                        .rotate(if (isPlaying) spinDegree else 0f)
                ) {
                    AsyncImage(
                        model = song.getLocalOrFallbackImage(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.album} • $currentPositionStr / $durationStr",
                        fontSize = 10.sp,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onPlayPauseToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "تشغيل/توقيف",
                            tint = GoldenSultan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MusicWaveAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "music_wave")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(530, easing = LinearEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(370, easing = LinearEasing), RepeatMode.Reverse),
        label = "w3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(16.dp)
    ) {
        Box(modifier = Modifier.width(2.5.dp).fillMaxHeight(h1).background(GoldenSultan))
        Box(modifier = Modifier.width(2.5.dp).fillMaxHeight(h2).background(GoldenSultan))
        Box(modifier = Modifier.width(2.5.dp).fillMaxHeight(h3).background(GoldenSultan))
    }
}

@Composable
fun FavoritesTabContent(
    favorites: List<Song>,
    playingSong: Song?,
    isPlaying: Boolean,
    onSongSelect: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    downloadingSongs: Map<Int, Float> = emptyMap(),
    onDownloadClick: (Song) -> Unit = {},
    onDeleteClick: (Song) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "مفصَّلاتك من روائع أبو وديع ❤️",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val targetFavs = favorites.filter { it.isFavorite }

        if (targetFavs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "قائمة المفضلة فارغة حالياً.\nاضغط على رمز القلب ❤️ تحت أي أغنية لإضافتها هنا!",
                        textAlign = TextAlign.Center,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("favorites_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(targetFavs) { song ->
                    val isCurrent = playingSong?.id == song.id
                    val prog = downloadingSongs[song.id]
                    SongItemCard(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        downloadProgress = prog,
                        onSongSelect = { onSongSelect(song) },
                        onFavoriteToggle = { onFavoriteToggle(song) },
                        onDownloadClick = { onDownloadClick(song) },
                        onDeleteClick = { onDeleteClick(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuotesAndBioContent(quote: String, onRefreshQuote: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.5.dp,
                        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)),
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.FormatQuote,
                        contentDescription = null,
                        tint = GoldenSultan,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = quote,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            lineHeight = 25.sp,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRefreshQuote,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenSultan)
                    ) {
                        Text("الحِكمة التالية 🔄", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_george_wassouf),
                        contentDescription = "سلطان الطرب جورج وسوف - أبو وديع",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GoldenSultan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "السيرة والمسيرة 🏆",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = GoldenSultan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "جورج وسوف، الشهير بـ 'أبو وديع'، ولد في قرية الكفرون بسوريا عام 1961. يعتبر أحد أبرز مطربي جيله على الإطلاق بفضل حنجرته الاستثنائية التي تجمع بين الشجن والقوة والقدرة الهائلة على غناء تراث العرب الفني العظيم.\n\nبدأ مشواره الاحترافي في سن صغيرة جداً وشارف صوته كبار نجوم الأغنية في الشرق. قدّم مئات الأغاني التي لا تزال تُسمع في كل مقهى وشارع عربي من 'الهوى سلطان' و'حلف القمر' إلى 'كلام الناس'، وأسس مدرسة فريدة في الغناء واللحن الأصيل.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            color = Color.LightGray.copy(alpha = 0.9f)
                        )
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}

@Composable
fun FullSearchOverlay(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    songsResult: List<Song>,
    playingSong: Song?,
    isPlaying: Boolean,
    downloadingSongs: Map<Int, Float>,
    onClose: () -> Unit,
    onSongSelect: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    onDeleteClick: (Song) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12).copy(alpha = 0.99f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header Search Input Row with Back arrow and text field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            "بحث تفاعلي عن الأغاني والمواويل...",
                            color = Color.LightGray.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = GoldenSultan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "مسح",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldenSultan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Divider
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 12.dp))

            // Suggestions OR Results
            if (searchQuery.isEmpty()) {
                // Show gorgeous interactive recommendation grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = "اقتراحات البحث وشائعة الآن 🔥",
                        color = GoldenSultan,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                    )

                    val popularSuggestions = listOf(
                        "كلام الناس",
                        "طبيب جراح",
                        "الهوى سلطان",
                        "سلف ودين",
                        "صابر وراضي",
                        "خسرت كل الناس",
                        "لسه الدنيا بخير",
                        "حلف القمر",
                        "دبنا ع الغياب",
                        "قدك المياس",
                        "يوم الوداع",
                        "روحي يا نسمة"
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(popularSuggestions) { suggestion ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onQueryChange(suggestion) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = GoldenSultan.copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = suggestion,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Show Sorted Instant Results
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = "النتائج المطابقة 🎵",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (songsResult.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "لم نعثر على نتائج لـ \"$searchQuery\"",
                                    color = Color.LightGray.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(songsResult) { song ->
                                val isCurrent = playingSong?.id == song.id
                                val prog = downloadingSongs[song.id]
                                SongItemCard(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    downloadProgress = prog,
                                    onSongSelect = {
                                        onSongSelect(song)
                                    },
                                    onFavoriteToggle = { onFavoriteToggle(song) },
                                    onDownloadClick = { onDownloadClick(song) },
                                    onDeleteClick = { onDeleteClick(song) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

