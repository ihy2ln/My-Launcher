package com.homelauncher.app.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlayingInfo(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val packageName: String? = null,
    val appLabel: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progress: Float = 0f,
    val artwork: Bitmap? = null,
    val canSkip: Boolean = false,
    val hasSession: Boolean = false,
)

/**
 * Tracks the active [MediaController] via [MediaSessionManager].
 * Requires the notification listener to be enabled for session access.
 */
class NowPlayingRepository(private val context: Context) {

    private val manager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _nowPlaying = MutableStateFlow(NowPlayingInfo())
    val nowPlaying: StateFlow<NowPlayingInfo> = _nowPlaying.asStateFlow()

    private var activeController: MediaController? = null
    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() {
            detachController()
            refreshSessions()
        }
    }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { refreshSessions(it) }

    private val progressTicker = object : Runnable {
        override fun run() {
            val controller = activeController
            if (controller != null) {
                publish(fromTicker = true)
                if (_nowPlaying.value.isPlaying) {
                    mainHandler.postDelayed(this, 500L)
                }
            }
        }
    }

    fun start() {
        refreshSessions()
        runCatching {
            manager.addOnActiveSessionsChangedListener(
                sessionsListener,
                listenerComponent(),
                mainHandler,
            )
        }
    }

    fun stop() {
        mainHandler.removeCallbacks(progressTicker)
        runCatching {
            manager.removeOnActiveSessionsChangedListener(sessionsListener)
        }
        detachController()
    }

    fun isNotificationAccessEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        val cn = listenerComponent().flattenToString()
        return flat.split(":").any { it.equals(cn, ignoreCase = true) }
    }

    fun openNotificationListenerSettings() {
        val intent = android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun playPause() {
        val controller = activeController ?: return
        val state = controller.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun openSessionApp(): Boolean {
        val pkg = activeController?.packageName ?: return false
        val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        return true
    }

    fun refreshSessions(
        sessions: List<MediaController>? = null,
    ) {
        val list = sessions ?: runCatching {
            manager.getActiveSessions(listenerComponent())
        }.getOrDefault(emptyList())

        val preferred = list.firstOrNull { controller ->
            val state = controller.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        } ?: list.firstOrNull { it.playbackState != null || it.metadata != null }

        if (preferred?.sessionToken == activeController?.sessionToken) {
            publish()
            return
        }
        detachController()
        if (preferred != null) {
            activeController = preferred
            preferred.registerCallback(controllerCallback, mainHandler)
            publish()
        } else {
            _nowPlaying.value = NowPlayingInfo()
        }
    }

    private fun detachController() {
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        mainHandler.removeCallbacks(progressTicker)
    }

    private fun publish(fromTicker: Boolean = false) {
        val controller = activeController
        if (controller == null) {
            _nowPlaying.value = NowPlayingInfo()
            return
        }
        val metadata = controller.metadata
        val state = controller.playbackState
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
        val position = state?.position?.coerceAtLeast(0L) ?: 0L
        val playing = state?.state == PlaybackState.STATE_PLAYING
        val actions = state?.actions ?: 0L
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown title"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: controller.packageName
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val progress = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
        val label = runCatching {
            val ai = context.packageManager.getApplicationInfo(controller.packageName, 0)
            context.packageManager.getApplicationLabel(ai).toString()
        }.getOrNull()

        _nowPlaying.value = NowPlayingInfo(
            title = title,
            artist = artist.orEmpty(),
            album = album,
            packageName = controller.packageName,
            appLabel = label,
            isPlaying = playing,
            positionMs = position,
            durationMs = duration,
            progress = progress,
            artwork = art,
            canSkip = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            hasSession = true,
        )

        mainHandler.removeCallbacks(progressTicker)
        if (playing && !fromTicker) {
            mainHandler.postDelayed(progressTicker, 500L)
        }
    }

    private fun listenerComponent(): ComponentName =
        ComponentName(context, MediaNotificationListener::class.java)

    companion object {
        @Volatile private var instance: NowPlayingRepository? = null

        fun get(context: Context): NowPlayingRepository {
            return instance ?: synchronized(this) {
                instance ?: NowPlayingRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
