package com.example.ui.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(UnstableApi::class)
class AudioPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressTrackerJob: Job? = null

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        if (playing) {
                            startProgressTracker()
                        } else {
                            stopProgressTracker()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_READY -> {
                                _durationMs.value = duration
                            }
                            Player.STATE_ENDED -> {
                                _isPlaying.value = false
                                _playbackProgress.value = 1.0f
                                _currentPositionMs.value = duration
                                stopProgressTracker()
                            }
                            else -> { /* idle or buffering */ }
                        }
                    }
                })
            }
        }
    }

    fun playSong(song: Song) {
        initializePlayer()
        val player = exoPlayer ?: return

        _currentSong.value = song
        _playbackProgress.value = 0f
        _currentPositionMs.value = 0L
        _durationMs.value = 0L

        // Determine if we should play offline file or online remote url
        val mediaUri = if (song.downloadStatus == "COMPLETED" && song.localFilePath != null) {
            Log.d("AudioPlayer", "Playing downloaded offline song: ${song.arabicTitle} from ${song.localFilePath}")
            song.localFilePath
        } else {
            Log.d("AudioPlayer", "Playing online streaming song: ${song.arabicTitle} from ${song.remoteUrl}")
            song.remoteUrl
        }

        player.setMediaItem(MediaItem.fromUri(mediaUri))
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(progress: Float) {
        val player = exoPlayer ?: return
        val duration = _durationMs.value
        if (duration > 0) {
            val seekPosition = (progress * duration).toLong()
            player.seekTo(seekPosition)
            _currentPositionMs.value = seekPosition
            _playbackProgress.value = progress
        }
    }

    fun seekForward() {
        val player = exoPlayer ?: return
        val current = player.currentPosition
        val target = (current + 10000).coerceAtMost(player.duration)
        player.seekTo(target)
    }

    fun seekBackward() {
        val player = exoPlayer ?: return
        val current = player.currentPosition
        val target = (current - 10000).coerceAtLeast(0)
        player.seekTo(target)
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressTrackerJob = playerScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val current = player.currentPosition
                        val dur = player.duration.coerceAtLeast(1)
                        _currentPositionMs.value = current
                        _durationMs.value = dur
                        _playbackProgress.value = current.toFloat() / dur.toFloat()
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    fun release() {
        stopProgressTracker()
        playerScope.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
