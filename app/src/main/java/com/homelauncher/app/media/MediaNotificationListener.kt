package com.homelauncher.app.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Notification listener used as the privileged ComponentName for
 * [MediaSessionManager.getActiveSessions]. Also refreshes when media
 * notifications post/remove.
 */
class MediaNotificationListener : NotificationListenerService() {

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
        bindControllers(sessions.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishFromActive()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishFromActive()
        override fun onSessionDestroyed() = publishFromActive()
    }

    private var boundControllers: List<MediaController> = emptyList()

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        val msm = getSystemService(MediaSessionManager::class.java) ?: return
        val cn = ComponentName(this, MediaNotificationListener::class.java)
        msm.addOnActiveSessionsChangedListener(sessionListener, cn)
        bindControllers(msm.getActiveSessions(cn))
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        runCatching {
            getSystemService(MediaSessionManager::class.java)
                ?.removeOnActiveSessionsChangedListener(sessionListener)
        }
        clearControllers()
        if (instance === this) instance = null
        _state.value = NowPlayingState.Empty
        _listenerEnabled.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshSessions()
    }

    fun refreshSessions() {
        val msm = getSystemService(MediaSessionManager::class.java) ?: return
        val cn = ComponentName(this, MediaNotificationListener::class.java)
        bindControllers(msm.getActiveSessions(cn))
    }

    fun playPause() {
        val controller = preferredController() ?: return
        val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        if (playing) controller.transportControls.pause() else controller.transportControls.play()
    }

    fun skipNext() {
        preferredController()?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        preferredController()?.transportControls?.skipToPrevious()
    }

    private fun bindControllers(sessions: List<MediaController>) {
        clearControllers()
        boundControllers = sessions
        sessions.forEach { it.registerCallback(callback) }
        publishFromActive()
        _listenerEnabled.value = true
    }

    private fun clearControllers() {
        boundControllers.forEach { runCatching { it.unregisterCallback(callback) } }
        boundControllers = emptyList()
    }

    private fun preferredController(): MediaController? {
        val playing = boundControllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        return playing ?: boundControllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PAUSED
        } ?: boundControllers.firstOrNull()
    }

    private fun publishFromActive() {
        val controller = preferredController()
        if (controller == null) {
            _state.value = NowPlayingState.Empty
            return
        }
        val meta = controller.metadata
        val playback = controller.playbackState
        val pkg = controller.packageName
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrNull()
        val artwork = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val duration = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playback?.position ?: 0L
        val actions = playback?.actions ?: 0L
        _state.value = NowPlayingState(
            isActive = true,
            isPlaying = playback?.state == PlaybackState.STATE_PLAYING,
            title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
                .ifBlank { meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).orEmpty() },
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
                .ifBlank { meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty() },
            album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            packageName = pkg,
            appLabel = label,
            positionMs = position,
            durationMs = duration,
            canSkip = (actions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L,
            artwork = artwork,
        )
    }

    companion object {
        @Volatile
        var instance: MediaNotificationListener? = null
            private set

        private val _state = MutableStateFlow(NowPlayingState.Empty)
        val state: StateFlow<NowPlayingState> = _state.asStateFlow()

        private val _listenerEnabled = MutableStateFlow(false)
        val listenerEnabled: StateFlow<Boolean> = _listenerEnabled.asStateFlow()

        fun componentName(context: Context): ComponentName =
            ComponentName(context, MediaNotificationListener::class.java)

        fun isNotificationAccessEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ).orEmpty()
            val cn = ComponentName(context, MediaNotificationListener::class.java).flattenToString()
            return flat.split(':').any { it.equals(cn, ignoreCase = true) }
        }

        fun openNotificationAccessSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun playPause() = instance?.playPause()
        fun skipNext() = instance?.skipNext()
        fun skipPrevious() = instance?.skipPrevious()
        fun refresh() = instance?.refreshSessions()
    }
}
