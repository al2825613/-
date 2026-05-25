package com.example.ui.player

import android.content.Context
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

    private class ActiveNote(val job: Job, var track: AudioTrack?)
    private val activeSingleNotes = java.util.Collections.synchronizedList(mutableListOf<ActiveNote>())

    init {
        startProgressTracker()
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackMode.value = mode
        val song = _currentSong.value
        if (song != null && _isPlaying.value) {
            play(song)
        }
    }

    fun play(song: Song) {
        stopAll()
        _currentSong.value = song
        _duration.value = song.duration

        if (_playbackMode.value == PlaybackMode.SYNTH) {
            playSynthesizedMelody(song)
            return
        }

        // Stream mode
        try {
            mediaPlayer = MediaPlayer().apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                setAudioAttributes(audioAttributes)
                setDataSource(playerContext, Uri.parse(song.audioUrl))
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _duration.value = (mp.duration / 1000)
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
                
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(minBufferSize.coerceAtLeast(8192))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                
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
        val noteJob = playerScope.launch(Dispatchers.Default) {
            var track: AudioTrack? = null
            var activeNoteRef: ActiveNote? = null
            try {
                // Ensure we don't exceed 4 concurrent single note AudioTracks to prevent system pool exhaustion
                synchronized(activeSingleNotes) {
                    while (activeSingleNotes.size >= 4) {
                        val oldest = activeSingleNotes.removeAt(0)
                        oldest.job.cancel()
                        try {
                            oldest.track?.stop()
                        } catch (e: Exception) {}
                        try {
                            oldest.track?.release()
                        } catch (e: Exception) {}
                    }
                }

                val sampleRate = 44100
                val noteDurationMs = 600L
                val bgSize = (sampleRate * noteDurationMs / 1000).toInt()
                val noteBuffer = ShortArray(bgSize)
                
                // Build plucking wave
                for (i in noteBuffer.indices) {
                    val time = i.toDouble() / sampleRate
                    val wave = sin(2 * Math.PI * frequency * time)
                    // Classic guitar/Oud overtones (warm resonance)
                    val oct = 0.4 * sin(2 * Math.PI * (frequency * 2) * time)
                    val fifth = 0.2 * sin(2 * Math.PI * (frequency * 1.5) * time)
                    
                    // Exponential decay envelope
                    val envelope = Math.exp(-5.0 * i / bgSize)
                    noteBuffer[i] = ((wave + oct + fifth) / 1.6 * envelope * Short.MAX_VALUE).toInt().toShort()
                }
                
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                
                val audioFormat = AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .build()
                
                track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(noteBuffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                
                activeNoteRef = ActiveNote(coroutineContext[Job]!!, track)
                synchronized(activeSingleNotes) {
                    activeSingleNotes.add(activeNoteRef)
                }

                track.write(noteBuffer, 0, noteBuffer.size)
                track.play()
                delay(noteDurationMs + 100L)
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "playSingleNote error: ${e.message}")
            } finally {
                activeNoteRef?.let {
                    synchronized(activeSingleNotes) {
                        activeSingleNotes.remove(it)
                    }
                }
                try {
                    track?.stop()
                } catch (e: Exception) {
                    // Ignore
                }
                try {
                    track?.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun release() {
        stopAll()
        progressJob?.cancel()
        synchronized(activeSingleNotes) {
            for (note in activeSingleNotes) {
                note.job.cancel()
                try {
                    note.track?.stop()
                } catch (e: Exception) {}
                try {
                    note.track?.release()
                } catch (e: Exception) {}
            }
            activeSingleNotes.clear()
        }
        playerScope.cancel()
    }
}
