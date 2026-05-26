package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.theme.*
import com.example.ui.viewmodel.WassoufViewModel

@Composable
fun MainScreen(viewModel: WassoufViewModel) {
    val songs by viewModel.songsList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    // Enforce Arabic RTL directionality
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                HeaderSection()
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search layout
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) }
                    )

                    // Categories slider
                    CategoryTabs(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic LazyColumn listing songs
                    if (songs.isEmpty()) {
                        EmptyState(query = searchQuery)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = songs,
                                key = { _, song -> song.title }
                            ) { index, song ->
                                SongItemRow(
                                    index = index + 1,
                                    song = song,
                                    isCurrent = currentSong?.title == song.title,
                                    isPlaying = isPlaying && currentSong?.title == song.title,
                                    onPlayClick = { viewModel.playSong(song) },
                                    onFavoriteClick = { viewModel.toggleFavorite(song) }
                                )
                            }
                        }
                    }
                }

                // Sticky glassmorphic player card at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    PlayerControllerCard(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        position = currentPosition,
                        duration = duration,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onNextClick = { viewModel.playNext() },
                        onPrevClick = { viewModel.playPrevious() },
                        onSeek = { viewModel.seekTo(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = "أبو وديع الأسطورة",
            style = MaterialTheme.typography.labelMedium,
            color = GoldPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "سلطان الطرب جورج وسوف",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = SlateTextPrimary
            )
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Musical Theme",
                tint = GoldPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp)
            .testTag("search_field"),
        placeholder = {
            Text(
                text = "ابحث عن أغنية وديعة...",
                color = SlateTextSecondary,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = GoldPrimary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = SlateTextTertiary
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.textFieldColors(
            containerColor = DarkSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = GoldPrimary,
            focusedTextColor = SlateTextPrimary,
            unfocusedTextColor = SlateTextPrimary
        )
    )
}

@Composable
fun CategoryTabs(selectedCategory: String, onCategorySelected: (String) -> Unit) {
    val categories = listOf("الكل", "طرب", "شجن", "رومانسيات")
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories.size) { index ->
            val cat = categories[index]
            val isSelected = selectedCategory == cat
            
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) DarkBackground else SlateTextSecondary,
                label = "textColor"
            )
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) GoldPrimary else DarkSurface,
                label = "containerColor"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(containerColor)
                    .clickable { onCategorySelected(cat) }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = cat,
                    color = contentColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SongItemRow(
    index: Int,
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val outlineColor by animateColorAsState(
        targetValue = if (isCurrent) GoldPrimary else DividerColor,
        label = "outline"
    )
    val cardBgColor by animateColorAsState(
        targetValue = if (isCurrent) DarkSurfaceVariant else DarkSurface,
        label = "bgColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBgColor)
            .clickable { onPlayClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number or active playing icon status
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isCurrent) GoldPrimary.copy(alpha = 0.15f) else DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                    contentDescription = "Currently Played Status",
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = index.toString(),
                    color = SlateTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Song Information column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) GoldPrimary else SlateTextPrimary,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "جورج وسوف",
                    color = SlateTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Small Category Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = song.category,
                        color = GoldPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Action Toggles
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite Toggle",
                tint = if (song.isFavorite) Color.Red else SlateTextTertiary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun PlayerControllerCard(
    currentSong: Song?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkSurfaceVariant,
                        DarkSurface.copy(alpha = 0.95f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (currentSong != null) {
                // Song Metadata row inside controller
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = "Album Art",
                            tint = GoldPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong.title,
                            color = SlateTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "السلطان • جاري التشغيل بالخلفية",
                            color = GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress SeekBar
                val sliderValue = if (duration > 0) position.toFloat() else 0f
                val maxLimit = if (duration > 0) duration.toFloat() else 1f

                Slider(
                    value = sliderValue,
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..maxLimit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .testTag("song_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = GoldPrimary,
                        activeTrackColor = GoldPrimary,
                        inactiveTrackColor = DividerColor
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(position),
                        color = SlateTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatTime(duration),
                        color = SlateTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            } else {
                // No item selected banner
                Text(
                    text = "اختر أغنية لتطرب مسامعك بأعذب الألحان",
                    color = SlateTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Primary control buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = if (currentSong != null) SlateTextPrimary else SlateTextTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Big play button animation scale
                val playButtonScale by animateFloatAsState(
                    targetValue = if (isPlaying) 1.05f else 1f,
                    label = "btnScale"
                )

                IconButton(
                    onClick = { if (currentSong != null) onPlayPauseClick() },
                    modifier = Modifier
                        .scale(playButtonScale)
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(if (currentSong != null) GoldPrimary else SlateTextTertiary)
                        .testTag("play_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause Button",
                        tint = DarkBackground,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = if (currentSong != null) SlateTextPrimary else SlateTextTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QueueMusic,
            contentDescription = "Empty music list",
            tint = SlateTextTertiary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (query.isNotEmpty()) "لم نعثر على أغنية تطابق: \"$query\"" else "لا توجد أغاني بقائمة التشغيل المحددة",
            style = MaterialTheme.typography.titleLarge,
            color = SlateTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "تأكد من كتابة أحرف الكلمة بشكل صحيح، أو أضف المزيد لقائمة songs.json لتظهر تلقائياً طرباً لا ينقطع.",
            style = MaterialTheme.typography.bodyMedium,
            color = SlateTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
