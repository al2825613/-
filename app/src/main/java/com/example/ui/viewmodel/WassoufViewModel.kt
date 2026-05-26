package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.WassoufDatabase
import com.example.data.model.Song
import com.example.data.repository.SongRepository
import com.example.ui.player.AudioPlayerManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WassoufViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SongRepository
    val playerManager: AudioPlayerManager

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    // Active Tab state
    private val _activeTab = MutableStateFlow("songs") // "songs", "lyrics", "oud", "quotes"
    val activeTab = _activeTab.asStateFlow()

    // Quotes state
    val wassoufQuotes = listOf(
        "كلام الناس لا بيقدم ولا يأخر، المهم دايماً تخلّي راسك مرفوعة.",
        "الدنيا دي سلف ودين، اللي بتقدمه لأخوك بكرة تلاقيه في طريقك.",
        "طبيب جراح أداوي قلوب الناس، بس جرح قلبي ما يطيب إلا بالرضا.",
        "الصبر هو رفيق العمر والي، وأنا دايماً صابر وراضي بـ المكتوب.",
        "الفن الصادق هو اللي بينبع من الوجدان ويوصل دغري لقلوب السميّعة.",
        "يا حبايب أبو وديع، عيشوا بالحب والصفا لأن العمر رايح وما بيبقى إلا الذكرى الطيبة.",
        "الهوى سلطان يا عاشقين، بس أهضم شي هو الوفاء والإخلاص الصادق.",
        "لسه الدنيا بخير طول ما في قلوب بتحب وبتسامح وبتتمنى الخير لغيرها."
    )

    private val _currentQuote = MutableStateFlow(wassoufQuotes.first())
    val currentQuote = _currentQuote.asStateFlow()

    init {
        val database = WassoufDatabase.getDatabase(application)
        repository = SongRepository(database.songDao())
        playerManager = AudioPlayerManager(application)
        
        playerManager.onNextCallback = { slideNext(songsList.value) }
        playerManager.onPreviousCallback = { slidePrevious(songsList.value) }

        viewModelScope.launch {
            // Prepopulate Room DB with our beautiful tracklist
            repository.checkAndPrepopulate()
        }
    }

    // Reactive list of songs applying both search and category filters
    @OptIn(ExperimentalCoroutinesApi::class)
    val songsList: StateFlow<List<Song>> = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        if (query.isNotEmpty()) {
            repository.searchSongs(query)
        } else if (category != null) {
            repository.getSongsByCategory(category)
        } else {
            repository.allSongs
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player State forwards
    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying
    val currentPosition = playerManager.currentPosition
    val duration = playerManager.duration
    val playbackMode = playerManager.playbackMode

    fun setQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    fun playSong(song: Song) {
        playerManager.play(song)
        viewModelScope.launch {
            repository.incrementPlayCount(song.id)
        }
    }

    fun pauseSong() {
        playerManager.pause()
    }

    fun resumeSong() {
        playerManager.resume()
    }

    fun seekTo(seconds: Int) {
        playerManager.seekTo(seconds)
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, song.isFavorite)
        }
    }

    fun togglePlaybackMode() {
        val nextMode = if (playbackMode.value == AudioPlayerManager.PlaybackMode.STREAM) {
            AudioPlayerManager.PlaybackMode.SYNTH
        } else {
            AudioPlayerManager.PlaybackMode.STREAM
        }
        playerManager.setPlaybackMode(nextMode)
    }

    fun slideNext(songs: List<Song>) {
        val current = currentSong.value ?: return
        val currentIndex = songs.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < songs.size - 1) {
            playSong(songs[currentIndex + 1])
        } else if (songs.isNotEmpty()) {
            playSong(songs.first())
        }
    }

    fun slidePrevious(songs: List<Song>) {
        val current = currentSong.value ?: return
        val currentIndex = songs.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playSong(songs[currentIndex - 1])
        } else if (songs.isNotEmpty()) {
            playSong(songs.last())
        }
    }

    fun refreshQuote() {
        val remaining = wassoufQuotes.filter { it != _currentQuote.value }
        if (remaining.isNotEmpty()) {
            _currentQuote.value = remaining.random()
        }
    }

    fun playOudNote(hz: Double) {
        playerManager.playSingleNote(hz)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
