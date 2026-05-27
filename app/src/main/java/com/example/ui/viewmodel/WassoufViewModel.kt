package com.example.ui.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.WassoufDatabase
import com.example.data.model.Song
import com.example.data.repository.SongRepository
import com.example.ui.player.AudioPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

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
            // Prepopulate/Sync Room DB with our beautiful tracklist from assets/songs.json
            repository.checkAndPrepopulate(application)
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

    private val _downloadingSongs = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadingSongs = _downloadingSongs.asStateFlow()

    fun downloadSong(song: Song) {
        if (song.isDownloaded) {
            Toast.makeText(getApplication(), "الأغنية محملة بالفعل!", Toast.LENGTH_SHORT).show()
            return
        }
        if (_downloadingSongs.value.containsKey(song.id)) {
            Toast.makeText(getApplication(), "جاري تحميل هذه الأغنية بنجاح في الخلفية...", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _downloadingSongs.update { it + (song.id to 0.0f) }
            try {
                val urlToDownload = song.audioUrl.ifEmpty {
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                }

                val urlConnection = URL(urlToDownload).openConnection() as HttpURLConnection
                urlConnection.connectTimeout = 15000
                urlConnection.readTimeout = 15000
                urlConnection.connect()
                
                if (urlConnection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("رمز الاستجابة من المخدم: ${urlConnection.responseCode}")
                }
                
                val fileLength = urlConnection.contentLength
                val innerDir = File(getApplication<Application>().filesDir, "downloaded_songs")
                if (!innerDir.exists()) {
                    innerDir.mkdirs()
                }
                
                val cleanFileName = "song_${song.id}.mp3"
                val localFile = File(innerDir, cleanFileName)
                
                urlConnection.inputStream.use { input ->
                    FileOutputStream(localFile).use { output ->
                        val data = ByteArray(4096)
                        var total: Long = 0
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            if (fileLength > 0) {
                                val progress = total.toFloat() / fileLength
                                _downloadingSongs.update { it + (song.id to progress) }
                            }
                            output.write(data, 0, count)
                        }
                    }
                }
                
                repository.updateSongDownloadStatus(song.id, true, localFile.absolutePath)
                _downloadingSongs.update { it - song.id }
                
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "تم تحميل \"${song.title}\" بنجاح والمزامنة!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                _downloadingSongs.update { it - song.id }
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "فشل تحميل \"${song.title}\": ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun deleteDownloadedSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                song.localFilePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                repository.updateSongDownloadStatus(song.id, false, null)
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "تم حذف ملف \"${song.title}\" من الذاكرة بنجاح!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "فشل الحذف: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
