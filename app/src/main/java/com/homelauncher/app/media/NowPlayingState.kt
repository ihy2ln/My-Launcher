package com.homelauncher.app.media

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

/**
 * Snapshot of one system media session.
 */
data class NowPlayingState(
    val sessionKey: String = "",
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
    val canPrevious: Boolean = false,
    val artwork: Bitmap? = null,
    val brandColor: Long = 0xFF1DB954,
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val hasTrack: Boolean
        get() = title.isNotBlank() || artist.isNotBlank()

    companion object {
        val Empty = NowPlayingState()
    }
}

/** Brand colors keyed by media app package. */
fun brandColorForPackage(packageName: String?): Long {
    if (packageName.isNullOrBlank()) return 0xFF455A64
    val pkg = packageName.lowercase()
    return when {
        pkg.startsWith("com.spotify") -> 0xFF1DB954
        pkg.startsWith("com.google.android.youtube") || pkg.contains("youtube.music") -> 0xFFFF0000
        pkg.startsWith("com.maxmpz.audioplayer") -> 0xFFF5A623
        pkg.startsWith("tv.twitch") -> 0xFF9146FF
        pkg.startsWith("com.amazon.mp3") || pkg.startsWith("com.amazon.music") -> 0xFF232F3E
        pkg.startsWith("com.apple.android.music") -> 0xFFFA243C
        pkg.startsWith("com.soundcloud") -> 0xFFFF5500
        pkg.startsWith("com.pandora") -> 0xFF224099
        pkg.startsWith("deezer") -> 0xFFA238FF
        pkg.startsWith("com.aspiro.tidal") -> 0xFF000000
        pkg.startsWith("com.netflix") -> 0xFFE50914
        pkg.startsWith("com.google.android.apps.youtube.music") -> 0xFFFF0000
        pkg.startsWith("com.sec.android.app.music") -> 0xFF5C6BC0
        else -> 0xFF37474F
    }
}

fun Long.toComposeBrand(): Color = Color(this)
