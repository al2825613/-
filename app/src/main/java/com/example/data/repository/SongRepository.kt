package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.SongDao
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

class SongRepository(
    private val context: Context,
    private val songDao: SongDao
) {
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()

    suspend fun syncSongsWithJson() = withContext(Dispatchers.IO) {
        try {
            // Read songs.json from assets
            val inputStream = context.assets.open("songs.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            val jsonSongs = mutableListOf<Song>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val title = jsonObject.getString("title")
                val audioUrl = jsonObject.getString("audioUrl")
                
                // Grouping songs intelligently by keyword for categories
                val category = when {
                    title.contains("حب") || title.contains("عشق") || title.contains("حبيب") -> "رومانسيات"
                    title.contains("صبر") || title.contains("ألم") || title.contains("فراق") || title.contains("جرح") -> "شجن"
                    else -> "طرب"
                }
                
                jsonSongs.add(Song(title = title, audioUrl = audioUrl, category = category))
            }

            // Sync with Database
            for (jsonSong in jsonSongs) {
                val existingSong = songDao.getSongByTitle(jsonSong.title)
                if (existingSong == null) {
                    songDao.insertSongs(listOf(jsonSong))
                } else {
                    // Dynamic URL Update if edited in JSON, while preserving favorites/play counters
                    if (existingSong.audioUrl != jsonSong.audioUrl || existingSong.category != jsonSong.category) {
                        val updated = existingSong.copy(
                            audioUrl = jsonSong.audioUrl,
                            category = jsonSong.category
                        )
                        songDao.updateSong(updated)
                    }
                }
            }
            Log.d("SongRepository", "Synchronized ${jsonSongs.size} songs from JSON successfully.")
        } catch (e: Exception) {
            Log.e("SongRepository", "Error synchronizing songs.json: ${e.message}", e)
        }
    }

    suspend fun toggleFavorite(song: Song) {
        songDao.updateFavoriteStatus(song.title, !song.isFavorite)
    }

    suspend fun incrementPlayCount(song: Song) {
        songDao.incrementPlayCount(song.title, System.currentTimeMillis())
    }
}
