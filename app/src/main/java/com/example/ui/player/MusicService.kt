package com.example.ui.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var notificationManager: NotificationManager? = null

    companion object {
        const val CHANNEL_ID = "wassouf_music_channel"
        const val NOTIFICATION_ID = 4529
        
        const val ACTION_START = "com.example.wassouf.START"
        const val ACTION_UPDATE = "com.example.wassouf.UPDATE"
        const val ACTION_PLAY = "com.example.wassouf.PLAY"
        const val ACTION_PAUSE = "com.example.wassouf.PAUSE"
        const val ACTION_NEXT = "com.example.wassouf.NEXT"
        const val ACTION_PREVIOUS = "com.example.wassouf.PREVIOUS"
        const val ACTION_STOP = "com.example.wassouf.STOP"

        const val EXTRA_SONG_TITLE = "extra_song_title"
        const val EXTRA_ALBUM_TITLE = "extra_album_title"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_IMAGE_URL = "extra_image_url"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (intent == null || action == null) {
            // Guarantee startForeground gets called to prevent OS crash
            val fallbackSong = AudioPlayerManager.instance?.currentSong?.value
            val songTitle = fallbackSong?.title ?: "جورج وسوف"
            val albumTitle = fallbackSong?.album ?: "سلطان الطرب"
            val isPlaying = AudioPlayerManager.instance?.isPlaying?.value ?: false
            val imageUrl = fallbackSong?.imageUrl ?: ""
            showNotification(songTitle, albumTitle, isPlaying, imageUrl)
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_START, ACTION_UPDATE -> {
                val songTitle = intent.getStringExtra(EXTRA_SONG_TITLE) ?: "أغنية"
                val albumTitle = intent.getStringExtra(EXTRA_ALBUM_TITLE) ?: "ألبوم"
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL) ?: ""
                
                showNotification(songTitle, albumTitle, isPlaying, imageUrl)
            }
            ACTION_PLAY -> {
                AudioPlayerManager.instance?.resume()
            }
            ACTION_PAUSE -> {
                AudioPlayerManager.instance?.pause()
            }
            ACTION_NEXT -> {
                AudioPlayerManager.instance?.onNextCallback?.invoke()
            }
            ACTION_PREVIOUS -> {
                AudioPlayerManager.instance?.onPreviousCallback?.invoke()
            }
            ACTION_STOP -> {
                AudioPlayerManager.instance?.stop()
                try {
                    stopForeground(true)
                } catch (e: Exception) {
                    Log.e("MusicService", "Error stopping foreground: ${e.message}")
                }
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun showNotification(
        songTitle: String,
        albumTitle: String,
        isPlaying: Boolean,
        imageUrl: String
    ) {
        // Build base notification immediately to start foreground safely
        try {
            val initialNotification = buildNotification(songTitle, albumTitle, isPlaying, null)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Error starting foreground service: ${e.message}")
            try {
                // Fallback: try starting foreground without type in case of SecurityException/IllegalArgumentException
                val initialNotification = buildNotification(songTitle, albumTitle, isPlaying, null)
                startForeground(NOTIFICATION_ID, initialNotification)
            } catch (ex: Exception) {
                Log.e("MusicService", "Critical: Failed last-resort startForeground: ${ex.message}")
            }
        }

        // Asynchronously load custom album art if available
        if (imageUrl.isNotEmpty()) {
            serviceScope.launch {
                val bitmap = loadAlbumArt(imageUrl)
                if (bitmap != null) {
                    try {
                        val updatedNotification = buildNotification(songTitle, albumTitle, isPlaying, bitmap)
                        notificationManager?.notify(NOTIFICATION_ID, updatedNotification)
                    } catch (e: Exception) {
                        Log.e("MusicService", "Failed to notify updated notification: ${e.message}")
                    }
                }
            }
        }
    }

    private fun buildNotification(
        songTitle: String,
        albumTitle: String,
        isPlaying: Boolean,
        albumArt: Bitmap?
    ): Notification {
        // Intent to open Main App when clicking notification
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            openAppIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Action PendingIntents
        val prevPendingIntent = createActionPendingIntent(ACTION_PREVIOUS)
        val playPausePendingIntent = if (isPlaying) {
            createActionPendingIntent(ACTION_PAUSE)
        } else {
            createActionPendingIntent(ACTION_PLAY)
        }
        val nextPendingIntent = createActionPendingIntent(ACTION_NEXT)
        val stopPendingIntent = createActionPendingIntent(ACTION_STOP)

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(songTitle)
            .setContentText(albumTitle)
            .setSubText("سلطان الطرب جورج وسوف")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLargeIcon(albumArt)
            // Add media controls
            .addAction(android.R.drawable.ic_media_previous, "السابق", prevPendingIntent)
            .addAction(playPauseIcon, if (isPlaying) "إيقاف مؤقت" else "تشغيل", playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "التالي", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", stopPendingIntent)

        // MediaStyle is omitted to prevent Class Verification errors and runtime linkage crashes across Android configurations.

        return builder.build()
    }

    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun loadAlbumArt(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val dataObj: Any = if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else if (url.isNotEmpty()) {
                val cleanPath = if (url.startsWith("images/")) url else "images/$url"
                "file:///android_asset/$cleanPath"
            } else {
                return@withContext null
            }
            val loader = Coil.imageLoader(this@MusicService)
            val request = ImageRequest.Builder(this@MusicService)
                .data(dataObj)
                .allowHardware(false) // Request hardware=false to acquire standalone Bitmap for Notification
                .build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            (result as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            Log.e("MusicService", "Failed to load album art bitmap: ${e.message}")
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تشغيل الأغاني"
            val descriptionText = "قناة التحكم في تشغيل أغاني جورج وسوف بالخلفية"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
