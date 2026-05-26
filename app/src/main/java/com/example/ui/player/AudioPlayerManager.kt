package com.example.ui.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

class AudioPlayerManager(private val context: Context) {

    companion object {
        var instance: AudioPlayerManager? = null
    }

    var onNextCallback: (() -> Unit)? = null
    var onPreviousCallback: (() -> Unit)? = null

    private val playerContext = context

    private var mediaPlayer: MediaPlayer? = null
    
    // Synth engine for offline/failsafe playbacks or instrumental sessions
    private var synthJob: Job? = null
    private var isSynthPlaying = false
    private var isMuted = false

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration = _duration.asStateFlow()

    private val _playbackMode = MutableStateFlow(PlaybackMode.STREAM) // STREAM or SYNTH
    val playbackMode = _playbackMode.asStateFlow()

    enum class PlaybackMode {
        STREAM, SYNTH
    }

    private var progressJob: Job? = null
    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private class PluckState(
        val freq: Double,
        var currentSample: Int,
        val totalSamples: Int
    )
    private val activePlucks = java.util.Collections.synchronizedList(mutableListOf<PluckState>())
    private var mixerJob: Job? = null

    init {
        instance = this
        startProgressTracker()
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackMode.value = mode
        val song = _currentSong.value
        if (song != null && _isPlaying.value) {
            play(song)
        }
    }

    private fun getFallbackAudioUrl(song: Song): String {
        return when (song.title) {
            "كلام الناس" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            "طبيب جراح" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
            "صابر وراضي" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
            "الهوى سلطان" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
            "سلف ودين" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
            "خسرت كل الناس" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3"
            "حلف القمر" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3"
            "لسه الدنيا بخير" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"
            "لو نويت" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3"
            "حبيبي كده" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3"
            "روحي يا نسمة" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3"
            "حد ينسى قلبه" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3"
            "يوم الوداع" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3"
            "شي غريب" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3"
            "بتعتب عليّ" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3"
            "انت غيرهم" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            "قلب عاشق دليله" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
            "شكراً" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
            else -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        }
    }

    fun play(song: Song) {
        stopAll()
        _currentSong.value = song
        _duration.value = song.duration

        if (_playbackMode.value == PlaybackMode.SYNTH) {
            playSynthesizedMelody(song)
            updateNotification()
            return
        }

        // Stream or local asset mode
        try {
            mediaPlayer = MediaPlayer().apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                setAudioAttributes(audioAttributes)

                val urlToPlay = song.audioUrl
                if (urlToPlay.startsWith("http://") || urlToPlay.startsWith("https://")) {
                    setDataSource(playerContext, Uri.parse(urlToPlay))
                } else {
                    // Try playing from local asset folders
                    val potentialPaths = listOf(
                        urlToPlay,
                        if (urlToPlay.startsWith("songs/")) urlToPlay else "songs/$urlToPlay",
                        "songs/${song.englishTitle.replace(" ", "_").lowercase()}.mp3",
                        "songs/${song.englishTitle.replace(" ", "").lowercase()}.mp3"
                    )
                    var foundLocal = false
                    for (path in potentialPaths) {
                        if (path.isEmpty()) continue
                        try {
                            val afd = playerContext.assets.openFd(path)
                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                            foundLocal = true
                            Log.d("AudioPlayerManager", "Playing from local asset: $path")
                            break
                        } catch (e: Exception) {
                            // Try next path candidate
                        }
                    }
                    if (!foundLocal) {
                        // Fallback to online url if local is missing on disk
                        val fallbackUrl = getFallbackAudioUrl(song)
                        Log.d("AudioPlayerManager", "Local asset file not found. Playing fallback URL: $fallbackUrl")
                        setDataSource(playerContext, Uri.parse(fallbackUrl))
                    }
                }

                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _duration.value = (mp.duration / 1000)
                    updateNotification()
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = 0
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: $what, $extra. Falling back to synth.")
                    // If streaming fails (e.g., offline), fallback to synthesized Arabic acoustic notes so it always operates
                    setPlaybackMode(PlaybackMode.SYNTH)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error starting media player: ${e.message}. Falling back to synth.")
            setPlaybackMode(PlaybackMode.SYNTH)
        }
    }

    fun pause() {
        if (_playbackMode.value == PlaybackMode.SYNTH) {
            isSynthPlaying = false
            synthJob?.cancel()
            _isPlaying.value = false
        } else {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                }
            }
        }
        updateNotification()
    }

    fun resume() {
        val song = _currentSong.value ?: return
        if (_playbackMode.value == PlaybackMode.SYNTH) {
            playSynthesizedMelody(song)
        } else {
            mediaPlayer?.let {
                it.start()
                _isPlaying.value = true
            } ?: play(song)
        }
        updateNotification()
    }

    fun stop() {
        stopAll()
        _currentSong.value = null
    }

    fun seekTo(seconds: Int) {
        if (_playbackMode.value == PlaybackMode.SYNTH) {
            _currentPosition.value = seconds.coerceIn(0, _duration.value)
        } else {
            mediaPlayer?.let {
                it.seekTo(seconds * 1000)
                _currentPosition.value = seconds
            }
        }
    }

    private fun stopAll() {
        // Stop MediaPlayer
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
            } catch (e: Exception) {
                // Ignore
            }
            it.release()
        }
        mediaPlayer = null
        
        // Stop Custom Synthesizer
        isSynthPlaying = false
        synthJob?.cancel()
        synthJob = null

        _isPlaying.value = false
        stopNotificationService()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = playerScope.launch {
            while (isActive) {
                delay(1000)
                if (_isPlaying.value) {
                    if (_playbackMode.value == PlaybackMode.STREAM) {
                        mediaPlayer?.let {
                            if (it.isPlaying) {
                                _currentPosition.value = it.currentPosition / 1000
                            }
                        }
                    } else {
                        // Synth mode progresses manually
                        val nextPos = _currentPosition.value + 1
                        if (nextPos >= _duration.value) {
                            _currentPosition.value = 0
                            _isPlaying.value = false
                        } else {
                            _currentPosition.value = nextPos
                        }
                    }
                }
            }
        }
    }

    /**
     * Synthesizes classical Arabic acoustic notes/loops in a separate thread so it works beautifully offline.
     */
    private fun playSynthesizedMelody(song: Song) {
        synthJob?.cancel()
        isSynthPlaying = true
        _isPlaying.value = true
        
        synthJob = playerScope.launch(Dispatchers.Default) {
            var audioTrack: AudioTrack? = null
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                
                val audioFormat = AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .build()
                
                val trackBuilder = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(minBufferSize.coerceAtLeast(8192))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                audioTrack = trackBuilder.build()
                
                audioTrack.play()
                
                // Define classical Arab Tarab chord progressions (frequencies in Hz corresponding to maqams like Bayati or Rast)
                val baseNotes = when (song.title) {
                    "كلام الناس" -> doubleArrayOf(293.66, 329.63, 349.23, 392.00, 440.00, 493.88, 523.25) // D, E half-flat, F, G, A, B-flat, C (D Bayati)
                    "طبيب جراح" -> doubleArrayOf(261.63, 293.66, 311.13, 349.23, 392.00, 415.30, 466.16) // C Minor
                    "الهوى سلطان" -> doubleArrayOf(261.63, 293.66, 329.63, 349.23, 392.00, 440.00, 493.88) // C Major / Rast
                    else -> doubleArrayOf(293.66, 349.23, 392.00, 440.00, 493.88, 587.33) // D Minor pentatonic
                }
                
                var noteIndex = 0
                val noteDurationMs = 800L
                val noteBuffer = ShortArray((sampleRate * noteDurationMs / 1000).toInt())
                
                while (isSynthPlaying && isActive) {
                    val freq = baseNotes[noteIndex % baseNotes.size]
                    noteIndex++
                    
                    // Synthesize beautiful acoustic string vibe (sine wave with a rich guitar-like decay envelope)
                    for (i in noteBuffer.indices) {
                        val time = i.toDouble() / sampleRate
                        // Primary frequency string
                        val wave = sin(2 * Math.PI * freq * time)
                        // Warm guitar-like harmonic overtones
                        val overtone1 = 0.5 * sin(2 * Math.PI * (freq * 2) * time)
                        val overtone2 = 0.25 * sin(2 * Math.PI * (freq * 3) * time)
                        
                        // Decay envelope to sound like an Oud string pluck
                        val envelope = Math.exp(-4.0 * i / noteBuffer.size)
                        val sampleValue = ((wave + overtone1 + overtone2) / 1.75 * envelope * Short.MAX_VALUE)
                        
                        noteBuffer[i] = sampleValue.toInt().toShort()
                    }
                    
                    audioTrack.write(noteBuffer, 0, noteBuffer.size)
                    delay(noteDurationMs)
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Synth write error: ${e.message}")
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Synthesize and immediately play a single beautiful live acoustic Note (for the Virtual Oud interactable!).
     */
    fun playSingleNote(frequency: Double) {
        val sampleRate = 44100
        val durationMs = 600L
        val totalSamples = (sampleRate * durationMs / 1000).toInt()
        val newPluck = PluckState(frequency, 0, totalSamples)
        
        synchronized(activePlucks) {
            activePlucks.add(newPluck)
        }
        
        startMixerIfNeeded()
    }

    private fun startMixerIfNeeded() {
        synchronized(this) {
            if (mixerJob == null || mixerJob?.isActive == false) {
                mixerJob = playerScope.launch(Dispatchers.Default) {
                    runMixerLoop()
                }
            }
        }
    }

    private suspend fun runMixerLoop() {
        val sampleRate = 44100
        val bufferSize = 2048
        val buffer = ShortArray(bufferSize)
        
        var track: AudioTrack? = null
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            
            val audioFormat = AudioFormat.Builder()
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .build()
            
            val trackBuilder = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(minBufferSize.coerceAtLeast(bufferSize * 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
            track = trackBuilder.build()
            
            track.play()
            
            while (currentCoroutineContext().isActive) {
                var hasPlucks = false
                val localPlucks = mutableListOf<PluckState>()
                synchronized(activePlucks) {
                    if (activePlucks.isNotEmpty()) {
                        localPlucks.addAll(activePlucks)
                        hasPlucks = true
                    }
                }
                
                if (!hasPlucks) {
                    break
                }
                
                buffer.fill(0)
                
                for (i in 0 until bufferSize) {
                    var mixedSample = 0.0
                    val iterator = localPlucks.iterator()
                    while (iterator.hasNext()) {
                        val pluck = iterator.next()
                        if (pluck.currentSample >= pluck.totalSamples) {
                            synchronized(activePlucks) {
                                activePlucks.remove(pluck)
                            }
                            iterator.remove()
                            continue
                        }
                        
                        val time = pluck.currentSample.toDouble() / sampleRate
                        val wave = sin(2 * Math.PI * pluck.freq * time)
                        val oct = 0.4 * sin(2 * Math.PI * (pluck.freq * 2) * time)
                        val fifth = 0.2 * sin(2 * Math.PI * (pluck.freq * 1.5) * time)
                        
                        val envelope = Math.exp(-5.0 * pluck.currentSample / pluck.totalSamples)
                        mixedSample += ((wave + oct + fifth) / 1.6 * envelope)
                        
                        pluck.currentSample++
                    }
                    
                    val clampedSample = (mixedSample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt()
                    buffer[i] = clampedSample.toShort()
                }
                
                track.write(buffer, 0, bufferSize)
                yield()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Mixer loop error: ${e.message}")
        } finally {
            try {
                track?.stop()
            } catch (e: Exception) {}
            try {
                track?.release()
            } catch (e: Exception) {}
        }
    }

    fun release() {
        stopAll()
        progressJob?.cancel()
        mixerJob?.cancel()
        synchronized(activePlucks) {
            activePlucks.clear()
        }
        playerScope.cancel()
    }

    private fun updateNotification() {
        val song = _currentSong.value ?: return
        val isPlayingValue = _isPlaying.value
        try {
            val intent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_START
                putExtra(MusicService.EXTRA_SONG_TITLE, song.title)
                putExtra(MusicService.EXTRA_ALBUM_TITLE, song.album)
                putExtra(MusicService.EXTRA_IS_PLAYING, isPlayingValue)
                putExtra(MusicService.EXTRA_IMAGE_URL, song.imageUrl)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to update MusicService: ${e.message}")
        }
    }

    private fun stopNotificationService() {
        try {
            val intent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_STOP
            }
            context.stopService(intent)
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to stop MusicService: ${e.message}")
        }
    }
}
