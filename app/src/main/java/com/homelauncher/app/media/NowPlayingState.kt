package com.homelauncher.app.media

import android.graphics.Bitmap

/**
 * Snapshot of the system media session currently playing (or last active).
 */
data class NowPlayingState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val packageName: String? = null,
    val appLabel: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val canSkip: Boolean = false,
    val artwork: Bitmap? = null,
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val hasTrack: Boolean
        get() = title.isNotBlank() || artist.isNotBlank()

    companion object {
        val Empty = NowPlayingState()
    }
}
