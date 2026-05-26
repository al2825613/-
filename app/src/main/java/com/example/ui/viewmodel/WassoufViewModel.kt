package com.example.ui.viewmodel

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.data.database.WassoufDatabase
import com.example.data.model.Song
import com.example.data.repository.SongRepository
import com.example.ui.player.AudioPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class WassoufViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WassoufDatabase.getDatabase(application)
    private val repository = SongRepository(application, db.songDao())

    init {
        // Self-initialize player to allocate background resources
        AudioPlayerManager.getPlayer(application)
        viewModelScope.launch {
            repository.syncSongsWithJson()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val songsList: StateFlow<List<Song>> = combine(
        repository.allSongs,
        _searchQuery,
        _selectedCategory
    ) { allSongs, query, category ->
        var list = allSongs
        if (category != "الكل") {
            list = list.filter { it.category == category }
        }
        if (query.isNotEmpty()) {
            list = list.filter { it.title.contains(query, ignoreCase = true) }
        }
        
        // Pass the loaded list directly to the Player playlist context
        AudioPlayerManager.setPlaylist(list)
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Connecting player live variables
    val currentSong: StateFlow<Song?> = AudioPlayerManager.currentSong
    val isPlaying: StateFlow<Boolean> = AudioPlayerManager.isPlaying
    val currentPosition: StateFlow<Long> = AudioPlayerManager.currentPosition
    val duration: StateFlow<Long> = AudioPlayerManager.duration
    val playlist: StateFlow<List<Song>> = AudioPlayerManager.playlist

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            repository.incrementPlayCount(song)
            AudioPlayerManager.playSong(song)
        }
    }

    fun togglePlayPause() {
        AudioPlayerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        AudioPlayerManager.seekTo(positionMs)
    }

    fun playNext() {
        AudioPlayerManager.playNext()
    }

    fun playPrevious() {
        AudioPlayerManager.playPrevious()
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song)
        }
    }
}
