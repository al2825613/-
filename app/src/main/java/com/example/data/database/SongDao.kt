package com.example.data.database

import androidx.room.*
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY year DESC, title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE title LIKE :query OR englishTitle LIKE :query OR lyrics LIKE :query OR album LIKE :query")
    fun searchSongs(query: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE category = :category")
    fun getSongsByCategory(category: String): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song): Long

    @Update
    suspend fun updateSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsList(): List<Song>

    @Query("UPDATE songs SET isFavorite = :isFav WHERE id = :songId")
    suspend fun updateFavoriteStatus(songId: Int, isFav: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Int)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongsCount(): Int
}
