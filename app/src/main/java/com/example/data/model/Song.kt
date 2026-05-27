package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val arabicTitle: String,
    val description: String,
    val durationText: String,
    val lyrics: String,
    val remoteUrl: String,
    val localFilePath: String? = null,
    val isFavorite: Boolean = false,
    val downloadProgress: Int = 0, // 0 to 100
    val downloadStatus: String = "NOT_DOWNLOADED" // NOT_DOWNLOADED, DOWNLOADING, COMPLETED, FAILED
)
