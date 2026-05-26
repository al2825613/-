package com.example.ui.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
object AudioPlayerManager {
    private var exoPlayer: ExoPlayer? = null
    
    // UI reactive states
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private var progressJob: Job? = null
    private val managerScope = CoroutineScope(Dispatchers.Main + Job())

    fun getPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            val cache = AudioCacheManager.getCache(context.applicationContext)
            // Use stream-oriented, low latency default http data source
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)

            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val mediaSourceFactory = DefaultMediaSourceFactory(context.applicationContext)
                .setDataSourceFactory(cacheDataSourceFactory)

            exoPlayer = ExoPlayer.Builder(context.applicationContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .setHandleAudioBecomingNoisy(true) // pauses playback when headphone unplugged
                .build()

            exoPlayer?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> {
                            _duration.value = exoPlayer?.duration ?: 0L
                        }
                        Player.STATE_ENDED -> {
                            playNext()
                        }
                        else -> {}
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val url = mediaItem?.requestMetadata?.mediaUri?.toString() ?: ""
                    val matchingSong = _playlist.value.find { it.audioUrl == url }
                    if (matchingSong != null) {
                        _currentSong.value = matchingSong
                    }
                }
            })
        }
        return exoPlayer!!
    }

    fun setPlaylist(songs: List<Song>) {
        _playlist.value = songs
    }

    fun playSong(song: Song) {
        val player = exoPlayer ?: return
        _currentSong.value = song

        val currentMediaItem = player.currentMediaItem
        val currentUrl = currentMediaItem?.requestMetadata?.mediaUri?.toString()

        if (currentUrl == song.audioUrl) {
            if (!player.isPlaying) {
                player.play()
            }
            return
        }

        // Add proper media metadata for lock screen integration, system drawer
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist("جورج وسوف")
            .setAlbumTitle("أبو وديع الأسطورة")
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(song.audioUrl)
            .setMediaMetadata(metadata)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(Uri.parse(song.audioUrl))
                    .build()
            )
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0)
            }
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun playNext() {
        val current = _currentSong.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.title == current.title }
        if (currentIndex != -1) {
            val nextIndex = (currentIndex + 1) % list.size
            playSong(list[nextIndex])
        }
    }

    fun playPrevious() {
        val current = _currentSong.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.title == current.title }
        if (currentIndex != -1) {
            val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
            playSong(list[prevIndex])
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = managerScope.launch {
            while (true) {
                _currentPosition.value = exoPlayer?.currentPosition ?: 0L
                _duration.value = exoPlayer?.duration ?: 0L
                delay(500) // Update twice a second for ultra-fluid SeekBar
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}
