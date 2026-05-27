package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Song
import com.example.ui.theme.*
import com.example.ui.viewmodel.WassoufViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: WassoufViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Library, 1 = Offline, 2 = Favorites
    var isExpandedPlayerVisible by remember { mutableStateOf(false) }
    var showSplashScreen by remember { mutableStateOf(true) }

    val listToShow = when (selectedTab) {
        0 -> allSongs
        1 -> downloadedSongs
        else -> favoriteSongs
    }

    if (showSplashScreen) {
        WassoufSplashScreen(onDismiss = { showSplashScreen = false })
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBg)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Elegant Scrapbook Collage Header Banner (George Wassouf / Sultan Al-Tarab Theme)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                // Vintage Newspaper Collage Image Background
                AsyncImage(
                    model = R.drawable.img_wassouf_collage_1779875902192,
                    contentDescription = "Sultan Al-Tarab Scrapbook Collage",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark Scrim Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = GoldAccent.copy(alpha = 0.2f),
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .border(0.5.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                text = " أبو وديع ",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "سلطان الطرب",
                            color = TextPrimary,
                            style = MaterialTheme.typography.displayLarge,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "أغاني الأسطورة جورج وسوف كاملة مع الكلمات ودون اتصالات بالإنترنت.",
                            color = TextPrimary.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Stylized Golden Vinyl Visual Art in Header enclosing the singer's portrait
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(2.dp, GoldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = R.drawable.img_wassouf_square_1779877467246,
                            contentDescription = "Portrait",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Mini Golden Ring
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(1.dp, GoldAccent.copy(alpha = 0.8f), CircleShape)
                        )
                    }
                }
            }

            // Material 3 Custom Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = GoldAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldAccent
                    )
                },
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("المكتبة الموسيقية", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("المحملة (بدون نت)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = "Offline Files") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("المفضلة", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Songs list
            if (listToShow.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = when (selectedTab) {
                                1 -> Icons.Default.DownloadForOffline
                                2 -> Icons.Default.FavoriteBorder
                                else -> Icons.Default.MusicOff
                            },
                            contentDescription = "Empty icon",
                            tint = TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (selectedTab) {
                                1 -> "لم تقم بتحميل أي أغاني بعد.\nاضغط على أيقونة التحميل لحفظ الأغاني وسماعها دون إنترنت (بدون نت)!"
                                2 -> "لا توجد أغاني في قائمتك المفضلة حالياً."
                                else -> "جاري تحميل قائمة الأغاني..."
                            },
                            textAlign = TextAlign.Center,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("songs_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listToShow) { song ->
                        val isCurrent = currentSong?.id == song.id
                        SongRowItem(
                            song = song,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            onPlayClick = {
                                viewModel.playSong(song)
                                Toast.makeText(context, "جاري تشغيل: ${song.arabicTitle}", Toast.LENGTH_SHORT).show()
                            },
                            onFavoriteClick = {
                                viewModel.toggleFavorite(song)
                            },
                            onDownloadClick = {
                                if (song.downloadStatus == "COMPLETED") {
                                    // Option to delete
                                    viewModel.deleteDownload(song)
                                    Toast.makeText(context, "تم حذف الملف المحمل لتوفير مساحة.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.downloadSong(song)
                                    Toast.makeText(context, "بدأ تحميل أغنية: ${song.arabicTitle}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // Bottom Mini Player
            AnimatedVisibility(
                visible = currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                currentSong?.let { song ->
                    MiniPlayer(
                        song = song,
                        isPlaying = isPlaying,
                        progress = playbackProgress,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onMiniPlayerClick = { isExpandedPlayerVisible = true }
                    )
                }
            }
        }

        // Full Screen Aesthetic Player (Vinyl, seek bar, Arabic Lyrics)
        AnimatedVisibility(
            visible = isExpandedPlayerVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            currentSong?.let { song ->
                FullPlayerScreen(
                    song = song,
                    isPlaying = isPlaying,
                    progress = playbackProgress,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    onCloseClick = { isExpandedPlayerVisible = false },
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onSeek = { viewModel.seekTo(it) },
                    onSkipForward = { viewModel.seekForward() },
                    onSkipBackward = { viewModel.seekBackward() },
                    onFavoriteToggle = { viewModel.toggleFavorite(song) }
                )
            }
        }
    }
}
}

@Composable
fun SongRowItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) DarkSurfaceVariant else DarkSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .border(
                1.dp,
                if (isCurrent) GoldAccent.copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom Album Decorative Box displaying George Wassouf cover with Play status
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, if (isCurrent) GoldAccent else Color.Transparent, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // High visual quality portrait background
                AsyncImage(
                    model = R.drawable.img_wassouf_square_1779877467246,
                    contentDescription = "Song cover artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Dark translucent tint over the cover
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )

                if (isPlaying) {
                    // Active Waveform visual decoration
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Playing",
                        tint = GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isCurrent) GoldAccent else TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Titles
            Column(
                modifier = Modifier.weight(1.0f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = song.arabicTitle,
                        color = if (isCurrent) GoldAccent else TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    // Offline tag indicator (تخزين محلي)
                    if (song.downloadStatus == "COMPLETED") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = " بدون نت ",
                                color = Color(0xFF81C784),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Items (Favorite Red button, Download status indicator, duration marker)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Favorite Button
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Add to favorite",
                        tint = if (song.isFavorite) FavoriteRed else TextSecondary.copy(alpha = 0.6f)
                    )
                }

                // Download / Storage button
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    when (song.downloadStatus) {
                        "DOWNLOADING" -> {
                            CircularProgressIndicator(
                                progress = { song.downloadProgress / 100f },
                                modifier = Modifier.size(24.dp),
                                color = GoldAccent,
                                strokeWidth = 2.dp,
                            )
                        }
                        "COMPLETED" -> {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Downloaded successfully",
                                tint = Color(0xFF4CAF50)
                            )
                        }
                        "FAILED" -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Download failed",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.DownloadForOffline,
                                contentDescription = "Download for offline listening",
                                tint = TextSecondary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Text(
                    text = song.durationText,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onMiniPlayerClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { onMiniPlayerClick() }
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // mini vinyl rotation animation decoration
                val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(6000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotationDegrees"
                )

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .rotate(if (isPlaying) rotation else 0f)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(1.5.dp, GoldAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Rotating beautiful album cover inside miniplayer
                    AsyncImage(
                        model = R.drawable.img_wassouf_square_1779877467246,
                        contentDescription = "Rotating album cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Mini golden vinyl center hole
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color.Black, CircleShape)
                            .border(1.dp, GoldAccent.copy(alpha = 0.6f), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.arabicTitle,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    Text(
                        text = "جورج وسوف" + if (song.downloadStatus == "COMPLETED") " (محملة)" else " (بث مباشر)",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play or Pause",
                        tint = GoldAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Simple Linear Progress Bar Indicator at base of miniplayer
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = GoldAccent,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
fun FullPlayerScreen(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    currentPositionMs: Long,
    durationMs: Long,
    onCloseClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    var selectedPlayerTab by remember { mutableStateOf(0) } // 0 = Player, 1 = Lyrics (الكلمات)

    val formatTime = { ms: Long ->
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format("%02d:%02d", mins, secs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkSurfaceVariant,
                        DarkBg
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close player",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Inner player screen tab toggler (Player / Lyrics)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selectedPlayerTab == 0) GoldAccent else Color.Transparent)
                            .clickable { selectedPlayerTab = 0 }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "المشغل",
                            color = if (selectedPlayerTab == 0) DarkBg else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selectedPlayerTab == 1) GoldAccent else Color.Transparent)
                            .clickable { selectedPlayerTab = 1 }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "الكلمات",
                            color = if (selectedPlayerTab == 1) DarkBg else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Star favorite",
                        tint = if (song.isFavorite) FavoriteRed else TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedPlayerTab == 0) {
                // Tabs 1: Rotating Golden Vintage Vinyl Disk Visualizer
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(12000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "vinylRotation"
                    )

                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .rotate(if (isPlaying) rotation else 0f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF0A0A0A),
                                        Color(0xFF222222),
                                        Color(0xFF151515),
                                        Color(0xFF383838),
                                        Color(0xFF080808)
                                    )
                                )
                            )
                            .border(4.dp, GoldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Glossy Vinyl details
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .border(1.dp, Color(0xFF333333), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .border(1.5.dp, GoldAccent.copy(alpha = 0.35f), CircleShape)
                        )
                        // Middle vintage sticker label containing George's portrait
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, GoldAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = R.drawable.img_wassouf_square_1779877467246,
                                contentDescription = "George Wassouf vintage label",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Super subtle inner record holder hole
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color.Black, CircleShape)
                                    .border(1.dp, GoldAccent.copy(alpha = 0.6f), CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = song.arabicTitle,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GoldAccent.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = " أبو وديع ",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = song.title,
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }

                    // Online stream vs Offline tag
                    Spacer(modifier = Modifier.height(12.dp))
                    if (song.downloadStatus == "COMPLETED") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "مخزنة ومتاحة بدون إنترنت",
                                color = Color(0xFF81C784),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "بث مباشر (حملها لسماعها بدون نت)",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                // Tab 2: Aesthetic lyrics display section (كلمات الأغنية)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "كلمات الأغنية: ${song.arabicTitle}",
                        color = GoldAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = song.lyrics,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    lineHeight = 28.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Music Seekbar Progress controls
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Slider(
                    value = progress,
                    onValueChange = onSeek,
                    colors = SliderDefaults.colors(
                        thumbColor = GoldAccent,
                        activeTrackColor = GoldAccent,
                        inactiveTrackColor = TextSecondary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPositionMs),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(durationMs),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Play control icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Seek Back 10s
                IconButton(
                    onClick = onSkipBackward,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause core button with deep golden surface layout
                FloatingActionButton(
                    onClick = onPlayPauseClick,
                    containerColor = GoldAccent,
                    contentColor = DarkBg,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play or pause button",
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Seek Forward 10s
                IconButton(
                    onClick = onSkipForward,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10 seconds",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun WassoufSplashScreen(
    onDismiss: () -> Unit
) {
    var timerPercent by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val duration = 3000f // 3 seconds
        for (i in 1..100) {
            delay(15)
            timerPercent = i / 100f
        }
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF26231C), // Deep warm brown center
                        Color(0xFF0D0C09)  // Black charcoal background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top branding label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = "سُلْطَان الطَّرَب",
                    color = GoldAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Decorative divider line
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(1.5.dp)
                        .background(GoldAccent.copy(alpha = 0.5f))
                )
            }

            // Center portrait and spinner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large circular portrait enclosing George Wassouf
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .border(3.dp, GoldAccent, CircleShape)
                        .padding(6.dp)
                        .border(1.dp, GoldAccent.copy(alpha = 0.3f), CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = R.drawable.img_wassouf_square_1779877467246,
                        contentDescription = "George Wassouf portrait",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "جورج وسوف",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "بوابة الأنغام الطربية العريقة والخالدة",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Bottom loading progress and skip button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                // Custom premium progress bar
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(timerPercent)
                            .background(GoldAccent)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Skip button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent.copy(alpha = 0.15f),
                        contentColor = GoldAccent
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = "دخول السلطنة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
