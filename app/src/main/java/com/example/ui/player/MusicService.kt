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
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import com.example.data.database.WassoufDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var notificationManager: NotificationManager? = null
    private var mediaSession: MediaSessionCompat? = null

    companion object {
        const val CHANNEL_ID = "wassouf_music_channel"
        const val NOTIFICATION_ID = 4529
        
        const val ACTION_START = "com.example.wassouf.START"
        const val ACTION_UPDATE = "com.example.wassouf.UPDATE"
        const val ACTION_PLAY = "com.example.wassouf.PLAY"
        const val ACTION_PAUSE = "com.example.wassouf.PAUSE"
        const val ACTION_NEXT = "com.example.wassouf.NEXT"
        const val ACTION_PREVIOUS = "com.example.wassouf.PREVIOUS"
        const val ACTION_FAVORITE = "com.example.wassouf.FAVORITE"
        const val ACTION_STOP = "com.example.wassouf.STOP"

        const val EXTRA_SONG_TITLE = "extra_song_title"
        const val EXTRA_ALBUM_TITLE = "extra_album_title"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_IMAGE_URL = "extra_image_url"
        const val EXTRA_IS_FAVORITE = "extra_is_favorite"
    }

    override fun getAttributionTag(): String? {
        return "wassouf_attribution"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // Setup MediaSessionCompat fully
        mediaSession = MediaSessionCompat(this, "WassoufMediaSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    AudioPlayerManager.instance?.resume()
                }
                override fun onPause() {
                    AudioPlayerManager.instance?.pause()
                }
                override fun onSkipToNext() {
                    AudioPlayerManager.instance?.onNextCallback?.invoke()
                }
                override fun onSkipToPrevious() {
                    AudioPlayerManager.instance?.onPreviousCallback?.invoke()
                }
                override fun onStop() {
                    AudioPlayerManager.instance?.stop()
                    stopSelf()
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (intent == null || action == null) {
            val fallbackSong = AudioPlayerManager.instance?.currentSong?.value
            val songTitle = fallbackSong?.title ?: "جورج وسوف"
            val albumTitle = fallbackSong?.album ?: "سلطان الطرب"
            val isPlaying = AudioPlayerManager.instance?.isPlaying?.value ?: false
            val imageUrl = fallbackSong?.imageUrl ?: ""
            val isFav = fallbackSong?.isFavorite ?: false
            showNotification(songTitle, albumTitle, isPlaying, imageUrl, isFav)
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_START, ACTION_UPDATE -> {
                val songTitle = intent.getStringExtra(EXTRA_SONG_TITLE) ?: "أغنية"
                val albumTitle = intent.getStringExtra(EXTRA_ALBUM_TITLE) ?: "ألبوم"
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL) ?: ""
                val isFav = intent.getBooleanExtra(EXTRA_IS_FAVORITE, false)
                
                showNotification(songTitle, albumTitle, isPlaying, imageUrl, isFav)
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
            ACTION_FAVORITE -> {
                val currentSong = AudioPlayerManager.instance?.currentSong?.value
                if (currentSong != null) {
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            val database = WassoufDatabase.getDatabase(this@MusicService)
                            val newFav = !currentSong.isFavorite
                            database.songDao().updateFavoriteStatus(currentSong.id, newFav)
                            AudioPlayerManager.instance?.updateCurrentSongFavorite(newFav)
                        } catch (e: Exception) {
                            Log.e("MusicService", "Failed to update favorite: ${e.message}")
                        }
                    }
                }
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
        imageUrl: String,
        isFavorite: Boolean
    ) {
        // Sync media session playback state
        try {
            val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP
                )
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1.0f
                )
            mediaSession?.setPlaybackState(stateBuilder.build())

            val metadataBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, songTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "جورج وسوف")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumTitle)
            mediaSession?.setMetadata(metadataBuilder.build())
        } catch (e: Exception) {
            Log.e("MusicService", "Error updating media session: ${e.message}")
        }

        // Build base notification immediately to start foreground safely
        try {
            val initialNotification = buildNotification(songTitle, albumTitle, isPlaying, isFavorite, null)
            
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
                // Fallback
                val initialNotification = buildNotification(songTitle, albumTitle, isPlaying, isFavorite, null)
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
                        val updatedNotification = buildNotification(songTitle, albumTitle, isPlaying, isFavorite, bitmap)
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
        isFavorite: Boolean,
        albumArt: Bitmap?
    ): Notification {
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
        val favPendingIntent = createActionPendingIntent(ACTION_FAVORITE)
        val stopPendingIntent = createActionPendingIntent(ACTION_STOP)

        // RemoteViews Spotify style design!
        val expandedView = RemoteViews(packageName, R.layout.notification_player)
        
        // Setup titles
        expandedView.setTextViewText(R.id.notification_title, songTitle)
        expandedView.setTextViewText(R.id.notification_artist, "جورج وسوف • $albumTitle")

        // Setup control pending intents
        expandedView.setOnClickPendingIntent(R.id.notification_prev, prevPendingIntent)
        expandedView.setOnClickPendingIntent(R.id.notification_play_pause, playPausePendingIntent)
        expandedView.setOnClickPendingIntent(R.id.notification_next, nextPendingIntent)
        expandedView.setOnClickPendingIntent(R.id.notification_favorite, favPendingIntent)
        expandedView.setOnClickPendingIntent(R.id.notification_close, stopPendingIntent)

        // Swap playback and favorite resources
        val playPauseRes = if (isPlaying) R.drawable.ic_pause_white else R.drawable.ic_play_white
        expandedView.setImageViewResource(R.id.notification_play_pause, playPauseRes)

        val favRes = if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outlined
        expandedView.setImageViewResource(R.id.notification_favorite, favRes)

        // Optional rounded image corner post-production
        val artBitmap = albumArt ?: BitmapFactory.decodeResource(resources, R.drawable.img_wassouf_fallback)
        if (artBitmap != null) {
            val roundedArt = getRoundedCornerBitmap(artBitmap, 16)
            expandedView.setImageViewBitmap(R.id.notification_album_art, roundedArt)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play_white)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .setCustomContentView(expandedView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        return builder.build()
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadiusDp: Int): Bitmap {
        val density = resources.displayMetrics.density
        val pixels = (cornerRadiusDp * density).toInt()

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = -0xbdbdbe
        canvas.drawRoundRect(rectF, pixels.toFloat(), pixels.toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
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
                .allowHardware(false)
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
        mediaSession?.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
