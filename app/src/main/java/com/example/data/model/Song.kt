package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val englishTitle: String,
    val album: String,
    val year: String,
    val lyrics: String,
    val duration: Int, // in seconds
    val audioUrl: String,
    val imageUrl: String,
    val category: String,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null
) : Serializable {
    fun getLocalOrFallbackImage(): Any {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl
        }
        if (imageUrl.isNotEmpty()) {
            val cleanPath = if (imageUrl.startsWith("images/")) imageUrl else "images/$imageUrl"
            return "file:///android_asset/$cleanPath"
        }
        return com.example.R.drawable.img_wassouf_fallback
    }
}
