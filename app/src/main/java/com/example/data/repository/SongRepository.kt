package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.SongDao
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

class SongRepository(private val songDao: SongDao) {

    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()

    fun searchSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongs("%$query%")
    }

    fun getSongsByCategory(category: String): Flow<List<Song>> {
        return songDao.getSongsByCategory(category)
    }

    suspend fun toggleFavorite(songId: Int, currentStatus: Boolean) {
        songDao.updateFavoriteStatus(songId, !currentStatus)
    }

    suspend fun incrementPlayCount(songId: Int) {
        songDao.incrementPlayCount(songId)
    }

    suspend fun checkAndPrepopulate(context: Context) {
        try {
            val jsonString = context.assets.open("songs.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            
            // Get all existing songs in Room DB
            val dbSongs = songDao.getAllSongsList()
            val parsedSongs = mutableListOf<Song>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val title = jsonObject.getString("title")
                val englishTitle = jsonObject.optString("englishTitle", "")
                val album = jsonObject.optString("album", "روائع أبو وديع")
                val year = jsonObject.optString("year", "N/A")
                val duration = jsonObject.optInt("duration", 240)
                val category = jsonObject.optString("category", "طرب")
                
                val audioUrl = jsonObject.optString("audioUrl", "")
                val imageUrl = jsonObject.optString("imageUrl", "")
                
                // Get lyrics default string from JSON
                var lyrics = jsonObject.optString("lyrics", "")
                
                // Scan local lyrics files dynamically inside assets/lyrics
                val potentialFileNames = if (englishTitle.isNotEmpty()) {
                    listOf(
                        "lyrics/${englishTitle.replace(" ", "_").lowercase()}.txt",
                        "lyrics/${englishTitle.replace(" ", "").lowercase()}.txt",
                        "lyrics/$englishTitle.txt",
                        "lyrics/$title.txt"
                    )
                } else {
                    listOf("lyrics/$title.txt")
                }
                
                for (fileName in potentialFileNames) {
                    try {
                        context.assets.open(fileName).use { stream ->
                            val text = stream.bufferedReader().use { it.readText() }
                            if (text.trim().isNotEmpty()) {
                                lyrics = text
                                Log.d("SongRepository", "Loaded updated lyrics from: $fileName")
                            }
                        }
                        break
                    } catch (e: Exception) {
                        // try next filename format
                    }
                }
                
                // Retain current favorite status and stats of existing tracks
                val existing = dbSongs.find { 
                    it.title == title || (englishTitle.isNotEmpty() && it.englishTitle.equals(englishTitle, ignoreCase = true)) 
                }
                
                val song = Song(
                    id = existing?.id ?: 0,
                    title = title,
                    englishTitle = englishTitle,
                    album = album,
                    year = year,
                    lyrics = lyrics,
                    duration = duration,
                    audioUrl = audioUrl,
                    imageUrl = imageUrl,
                    category = category,
                    isFavorite = existing?.isFavorite ?: false,
                    playCount = existing?.playCount ?: 0
                )
                
                parsedSongs.add(song)
                
                if (existing != null) {
                    songDao.updateSong(song)
                } else {
                    songDao.insertSong(song)
                }
            }
            
            // Sync deletions: remove tracks from Database that are no longer part of public JSON catalog
            for (dbSong in dbSongs) {
                val stillExists = parsedSongs.any { 
                    it.title == dbSong.title || (dbSong.englishTitle.isNotEmpty() && it.englishTitle.equals(dbSong.englishTitle, ignoreCase = true)) 
                }
                if (!stillExists) {
                    songDao.deleteSong(dbSong)
                    Log.d("SongRepository", "Desynchronized and removed track: ${dbSong.title}")
                }
            }
            Log.d("SongRepository", "Completed database sync with assets/songs.json. Total count: ${parsedSongs.size}")
        } catch (e: Exception) {
            Log.e("SongRepository", "Critical: Failed to read backend JSON/lyric assets: ${e.message}", e)
        }
    }
}
