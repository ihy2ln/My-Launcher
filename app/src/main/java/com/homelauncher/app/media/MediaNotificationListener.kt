package com.homelauncher.app.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Enables [android.media.session.MediaSessionManager.getActiveSessions] for this launcher.
 * Users must grant notification access in system settings.
 */
class MediaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        NowPlayingRepository.get(this).refreshSessions()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        NowPlayingRepository.get(this).refreshSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        NowPlayingRepository.get(this).refreshSessions()
    }
}
