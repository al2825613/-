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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Song
import com.example.ui.player.AudioPlayerManager
import com.example.ui.viewmodel.WassoufViewModel
import kotlinx.coroutines.isActive

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
    
    var isPlayerExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                // Persistent Mini Player if a song is loaded
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
                
                // Navigation Bottom Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = activeTab == "songs",
                        onClick = { viewModel.setActiveTab("songs") },
                        label = { Text("الأغاني", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Filled.MusicNote, contentDescription = "الأغاني") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == "lyrics",
                        onClick = { viewModel.setActiveTab("lyrics") },
                        label = { Text("الكلمات", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Filled.MenuBook, contentDescription = "الكلمات") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == "quotes",
                        onClick = { viewModel.setActiveTab("quotes") },
                        label = { Text("ملك الطرب", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Filled.InterpreterMode, contentDescription = "مقتبسات") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Majestic Royal Header
                HeaderSection(
                    quote = currentQuote,
                    showQuote = activeTab != "songs",
                    onQuoteTap = { viewModel.refreshQuote() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle tabs based on bottom navigation selection
                when (activeTab) {
                    "songs" -> {
                        SongsTabContent(
                            songs = songsList,
                            favorites = favoriteSongs,
                            searchQuery = viewModel.searchQuery.collectAsState().value,
                            selectedCategory = viewModel.selectedCategory.collectAsState().value,
                            playingSong = currentSong,
                            isPlaying = isPlaying,
                            onSearch = { viewModel.setQuery(it) },
                            onSongSelect = { viewModel.playSong(it) },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onSelectCategory = { viewModel.selectCategory(it) }
                        )
                    }
                    "lyrics" -> {
                        LyricsTabContent(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            songs = songsList,
                            onSongSelect = { viewModel.playSong(it) }
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

            // Exquisite Full Screen Music Player Overlay
            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
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
        }
    }
}

@Composable
fun HeaderSection(quote: String, showQuote: Boolean, onQuoteTap: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "أمــيــر الــطّــرب",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = "روائع جورج وسوف - أبو وديع",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (showQuote) {
            Spacer(modifier = Modifier.height(10.dp))

            // Interacting wisdom quotes card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { onQuoteTap() }
                    .shadow(4.dp, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "حِكمة أبو وديع (اضغط للتغيير)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = quote,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
    selectedCategory: String?,
    playingSong: Song?,
    isPlaying: Boolean,
    onSearch: (String) -> Unit,
    onSongSelect: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onSelectCategory: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontally Scrollable list of George Wassouf Albums
        Text(
            text = "تصفح ألبومات أبو وديع",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        
        val albums = listOf(
            Pair("الكل", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=300"),
            Pair("كلام الناس", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"),
            Pair("طبيب جراح", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300"),
            Pair("سلف ودين", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300"),
            Pair("الهوى سلطان", "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=300"),
            Pair("لسه الدنيا بخير", "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=300"),
            Pair("روائع وسنجلات", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=300")
        )

        var selectedAlbum by remember { mutableStateOf<String?>(null) }

        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(albums) { (albumName, albumCover) ->
                val isSelected = (albumName == "الكل" && selectedAlbum == null) || (selectedAlbum == albumName)
                Card(
                    modifier = Modifier
                        .width(105.dp)
                        .clickable {
                            selectedAlbum = if (albumName == "الكل") null else albumName
                        }
                        .border(
                            width = if (isSelected) 2.dp else 0.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = albumCover,
                            contentDescription = albumName,
                            modifier = Modifier
                                .size(65.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.DarkGray),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = albumName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Favorite Toggle Indicator
        var showOnlyFavorites by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .clickable { showOnlyFavorites = !showOnlyFavorites }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (showOnlyFavorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "عرض الأغاني المفضلة فقط",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Switch(
                checked = showOnlyFavorites,
                onCheckedChange = { showOnlyFavorites = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                ),
                modifier = Modifier.scale(0.8f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val albumFilteredSongs = if (selectedAlbum != null) {
            songs.filter { it.album == selectedAlbum }
        } else {
            songs
        }

        val displaySongs = if (showOnlyFavorites) {
            albumFilteredSongs.filter { it.isFavorite }
        } else {
            albumFilteredSongs
        }

        if (displaySongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.MusicOff,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (showOnlyFavorites) "لا توجد أغاني في المفضلة بعد.\nاضغط على رمز القلب لحفظ أغانيك المفضلة!" else "لم يتم العثور على أغانٍ مطابقة لبحثك.",
                        textAlign = TextAlign.Center,
                        color = Color.LightGray,
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
                    .testTag("songs_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displaySongs) { song ->
                    val isCurrent = playingSong?.id == song.id
                    SongItemCard(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        onSongSelect = { onSongSelect(song) },
                        onFavoriteToggle = { onFavoriteToggle(song) }
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
    onSongSelect: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrent) 1.5.dp else 0.5.dp,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .shadow(if (isCurrent) 6.dp else 2.dp, RoundedCornerShape(12.dp))
            .clickable { onSongSelect() }
            .testTag("song_item_card_${song.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art with vintage vinyl framing
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
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
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Custom wave indicator
                        MusicWaveAnimation()
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.album} • ${song.year}",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = song.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (song.playCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.Headset,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${song.playCount} استماع",
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Quick Actions: Favorite & Play icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    imageVector = if (isCurrent && isPlaying) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                    contentDescription = "تشغيل",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
fun MusicWaveAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "music_wave")
    val heightScale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_height_1"
    )
    val heightScale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_height_2"
    )
    val heightScale3 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_height_3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(18.dp)
    ) {
        Box(modifier = Modifier.width(2.5.dp).fillMaxHeight(heightScale).background(MaterialTheme.colorScheme.primary))
        Box(modifier = Modifier.width(2.5.dp).fillMaxHeight(heightScale2).background(MaterialTheme.colorScheme.primary))
        Box(modifier = Modifier.width(2.5.dp).fillMaxHeight(heightScale3).background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
fun LyricsTabContent(
    currentSong: Song?,
    isPlaying: Boolean,
    songs: List<Song>,
    onSongSelect: (Song) -> Unit
) {
    if (currentSong == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "الرجاء اختيار أغنية أولاً لعرض كلماتها.",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show brief quick-select songs lists
                Text("اختر أغنية سريعة للاستماع والكلمات:", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.height(180.dp).fillMaxWidth(0.8f)
                ) {
                    items(songs.take(3)) { song ->
                        Button(
                            onClick = { onSongSelect(song) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Text(song.title, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "كلمات: ${currentSong.title}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "من ألبوم ${currentSong.album} (${currentSong.year})",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
                if (isPlaying) {
                    MusicWaveAnimation()
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(12.dp))

            // Beautiful scannable typography lyric board with card frame
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Icon(
                            Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    val lines = currentSong.lyrics.split("\n")
                    items(lines) { line ->
                        if (line.trim().isEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                        } else {
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    lineHeight = 26.sp,
                                    color = if (line.contains("يا ملاكي") || line.contains("طبيب جراح") || line.contains("سلف ودين") || line.contains("صابر وراضي"))
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.White,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier
                                    .padding(vertical = 3.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VirtualOudContent(onPluckString: (Double) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                "آلة العود التفاعلية 🎼",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                "المس الأوتار للعزف التلقائي لأجمل الألحان الشرقية",
                fontSize = 11.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
        }

        // Virtual Oud Box representation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background artistic rosette/hole design
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val midX = size.width / 2
                    val midY = size.height / 2
                    
                    // Center sound hole (Sun)
                    drawCircle(
                        color = Color.Black,
                        radius = 80.dp.toPx(),
                        center = Offset(midX, midY)
                    )
                    // Golden lattice rim border
                    drawCircle(
                        color = Color(0xFFDFB15B),
                        radius = 83.dp.toPx(),
                        center = Offset(midX, midY),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }

                // 6 Oud Acoustic Strings
                val strings = listOf(
                    OudStringInfo("دو (C) القرار", 130.81, Color(0xFFDFB15B)),
                    OudStringInfo("صول (G)", 196.00, Color(0xFFE5A93B)),
                    OudStringInfo("ري (D)", 293.66, Color(0xFFFFD37D)),
                    OudStringInfo("لا (A)", 440.00, Color.White),
                    OudStringInfo("مي (E)", 329.63, Color(0xFFABABBA)),
                    OudStringInfo("دو (C) الجواب", 523.25, Color(0xFFF9E7BE))
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    strings.forEach { str ->
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .clickable { onPluckString(str.frequency) },
                            contentAlignment = Alignment.Center
                        ) {
                            // Vertical string visual representation
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(3.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                str.color,
                                                str.color.copy(alpha = 0.5f),
                                                str.color,
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .shadow(2.dp)
                            )
                            
                            // Floating Arabic label
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    str.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class OudStringInfo(val name: String, val frequency: Double, val color: Color)

@Composable
fun QuotesAndBioContent(quote: String, onRefreshQuote: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.FormatQuote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = quote,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRefreshQuote,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("الحكمة التالية 🔄", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_george_wassouf),
                        contentDescription = "أبو وديع - جورج وسوف",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "السيرة والمسيرة 🏆",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "جورج وسوف، الشهير بـ 'أبو وديع'، ولد في قرية الكفرون بسوريا عام 1961. يعتبر أحد أبرز مطربي جيله على الإطلاق بفضل حنجرته الاستثنائية التي تجمع بين الشجن والقوة والقدرة الهائلة على غناء تراث العرب الفني العظيم.\n\nبدأ مشواره الاحترافي في سن صغيرة جداً وشارف صوته كبار نجوم الأغنية في الشرق. قدّم مئات الأغاني التي لا تزال تُسمع في كل مقهى وشارع عربي من 'الهوى سلطان' و'حلف القمر' إلى 'كلام الناس'، وأسس مدرسة فريدة في الغناء واللحن الأصيل.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            color = Color.LightGray
                        )
                    )
                }
            }
        }
    }
}

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
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            // Tiny seek indicator line on top of mini player
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Disk visual running/rotating
                val infiniteTransition = rememberInfiniteTransition(label = "mini_disk")
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "disk_rotation"
                )

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .rotate(if (isPlaying) rotationAngle else 0f)
                ) {
                    AsyncImage(
                        model = song.getLocalOrFallbackImage(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Inner vinyl center
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.album} • $currentPositionStr / $durationStr",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onPlayPauseToggle) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                            contentDescription = "تشغيل/إيقاف مؤقت",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "تكبير",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.background,
                        Color.Black
                    )
                )
            )
            .padding(24.dp)
            .testTag("full_player")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Player Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "إغلاق",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "جاري التشغيل الآن",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "تفضيل",
                        tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Big Vinyl Record disc with rotation
            val discRotation = remember { Animatable(0f) }

            LaunchedEffect(isPlaying) {
                if (isPlaying) {
                    discRotation.animateTo(
                        targetValue = discRotation.value + 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(12000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                } else {
                    discRotation.stop()
                }
            }

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                    .graphicsLayer {
                        rotationZ = discRotation.value
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = song.getLocalOrFallbackImage(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                // Classic gold center label
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, Color.Black, CircleShape)
                )
            }

            // Metadata Card
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${song.album} • ${song.year}",
                    fontSize = 14.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                // Active Mode Toggle Widget (Stream vs Clean Local Acoustic notes)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onTogglePlaybackMode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (playbackMode == AudioPlayerManager.PlaybackMode.STREAM) Icons.Filled.CloudSync else Icons.Filled.Audiotrack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (playbackMode == AudioPlayerManager.PlaybackMode.STREAM) "بث مباشر (انترنت)" else "عزف محلي آلي (بدون انترنت)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Seek Controller & slider
            Column {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition), color = Color.Gray, fontSize = 12.sp)
                    Text(formatTime(duration), color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Player Commands Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "السابق",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "تشغيل الأغنية أو إيقاف المؤقت",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "التالي",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

// Format seconds to elegant M:SS
private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
