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
 * [MediaSessionManager.getActiveSessions]. Publishes *all* active sessions
 * so the drawer can show one themed card per player.
 */
class MediaNotificationListener : NotificationListenerService() {

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
        bindControllers(sessions.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishAll()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishAll()
        override fun onSessionDestroyed() = publishAll()
    }

    private var boundControllers: List<MediaController> = emptyList()

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        val msm = getSystemService(MediaSessionManager::class.java) ?: return
        val cn = ComponentName(this, MediaNotificationListener::class.java)
        msm.addOnActiveSessionsChangedListener(sessionListener, cn)
        bindControllers(msm.getActiveSessions(cn))
        refreshBadges()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        runCatching {
            getSystemService(MediaSessionManager::class.java)
                ?.removeOnActiveSessionsChangedListener(sessionListener)
        }
        clearControllers()
        if (instance === this) instance = null
        _sessions.value = emptyList()
        _state.value = NowPlayingState.Empty
        _listenerEnabled.value = false
        NotificationBadges.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshSessions()
        refreshBadges()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshSessions()
        refreshBadges()
    }

    private fun refreshBadges() {
        val active = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        val counts = active
            .filter { !it.isOngoing }
            .groupBy { it.packageName }
            .mapValues { (_, list) ->
                list.sumOf { sbn ->
                    val n = sbn.notification.number
                    if (n > 0) n else 1
                }
            }
        NotificationBadges.update(counts)
    }

    fun refreshSessions() {
        val msm = getSystemService(MediaSessionManager::class.java) ?: return
        val cn = ComponentName(this, MediaNotificationListener::class.java)
        bindControllers(msm.getActiveSessions(cn))
    }

    fun playPause(packageName: String? = null) {
        val controller = controllerFor(packageName) ?: return
        val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        if (playing) controller.transportControls.pause() else controller.transportControls.play()
    }

    fun skipNext(packageName: String? = null) {
        controllerFor(packageName)?.transportControls?.skipToNext()
    }

    fun skipPrevious(packageName: String? = null) {
        controllerFor(packageName)?.transportControls?.skipToPrevious()
    }

    private fun controllerFor(packageName: String?): MediaController? {
        if (!packageName.isNullOrBlank()) {
            val match = boundControllers.firstOrNull { it.packageName.equals(packageName, true) }
            if (match != null) return match
        }
        return preferredController()
    }

    private fun bindControllers(sessions: List<MediaController>) {
        clearControllers()
        // Deduplicate by package — keep the richest session per app
        boundControllers = sessions
            .groupBy { it.packageName }
            .map { (_, group) ->
                group.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                    ?: group.first()
            }
        boundControllers.forEach { it.registerCallback(callback) }
        publishAll()
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

    private fun publishAll() {
        val mapped = boundControllers.mapNotNull { toState(it) }
            .sortedByDescending { it.isPlaying }
        _sessions.value = mapped
        _state.value = mapped.firstOrNull() ?: NowPlayingState.Empty
    }

    private fun toState(controller: MediaController): NowPlayingState? {
        val meta = controller.metadata
        val playback = controller.playbackState
        val pkg = controller.packageName ?: return null
        val state = playback?.state ?: PlaybackState.STATE_NONE
        if (state == PlaybackState.STATE_NONE || state == PlaybackState.STATE_STOPPED ||
            state == PlaybackState.STATE_ERROR
        ) {
            // Keep paused/buffering sessions; drop fully idle ones without metadata
            if (meta == null) return null
        }
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrNull()
        val artwork = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val duration = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playback?.position ?: 0L
        val actions = playback?.actions ?: 0L
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
            .ifBlank { meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).orEmpty() }
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
            .ifBlank { meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty() }
        if (title.isBlank() && artist.isBlank() && state != PlaybackState.STATE_PLAYING) {
            return null
        }
        return NowPlayingState(
            sessionKey = "${pkg}:${System.identityHashCode(controller.sessionToken)}",
            isActive = true,
            isPlaying = state == PlaybackState.STATE_PLAYING,
            title = title.ifBlank { label ?: "Media" },
            artist = artist,
            album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            packageName = pkg,
            appLabel = label,
            positionMs = position,
            durationMs = duration,
            canSkip = (actions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L,
            canPrevious = (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L,
            artwork = artwork,
            brandColor = brandColorForPackage(pkg),
        )
    }

    companion object {
        @Volatile
        var instance: MediaNotificationListener? = null
            private set

        private val _state = MutableStateFlow(NowPlayingState.Empty)
        val state: StateFlow<NowPlayingState> = _state.asStateFlow()

        private val _sessions = MutableStateFlow<List<NowPlayingState>>(emptyList())
        val sessions: StateFlow<List<NowPlayingState>> = _sessions.asStateFlow()

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

        fun playPause(packageName: String? = null) = instance?.playPause(packageName)
        fun skipNext(packageName: String? = null) = instance?.skipNext(packageName)
        fun skipPrevious(packageName: String? = null) = instance?.skipPrevious(packageName)
        fun refresh() = instance?.refreshSessions()
    }
}
