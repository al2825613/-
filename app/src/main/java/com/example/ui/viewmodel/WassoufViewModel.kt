package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.WassoufDatabase
import com.example.data.model.Song
import com.example.data.repository.SongRepository
import com.example.ui.player.AudioPlayerManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WassoufViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WassoufDatabase.getDatabase(application)
    private val repository = SongRepository(application, database.songDao())
    val playerManager = AudioPlayerManager(application)

    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val downloadedSongs: StateFlow<List<Song>> = repository.downloadedSongs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val playbackProgress: StateFlow<Float> = playerManager.playbackProgress
    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs

    init {
        viewModelScope.launch {
            repository.initializeDefaultSongsIfNeeded()
        }
    }

    fun playSong(song: Song) {
        playerManager.playSong(song)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(progress: Float) {
        playerManager.seekTo(progress)
    }

    fun seekForward() {
        playerManager.seekForward()
    }

    fun seekBackward() {
        playerManager.seekBackward()
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, song.isFavorite)
            // Update currently playing reference if it is the same song
            if (currentSong.value?.id == song.id) {
                // Trigger live updates in current player view
            }
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            repository.downloadSong(song.id, song.remoteUrl)
        }
    }

    fun deleteDownload(song: Song) {
        viewModelScope.launch {
            repository.deleteDownloadedFile(song.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WassoufViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WassoufViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
