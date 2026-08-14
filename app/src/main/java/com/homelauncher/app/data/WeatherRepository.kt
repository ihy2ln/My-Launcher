package com.homelauncher.app.data

import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WeatherSnapshot(
    val tempF: Int,
    val condition: String,
    val locationLabel: String,
    val isLive: Boolean,
)

/**
 * Fetches current weather from Open-Meteo (no API key).
 * Uses last known location when available; otherwise a US default.
 */
object WeatherRepository {
    @Volatile
    private var cached: WeatherSnapshot? = null

    @Volatile
    private var cachedAtMs: Long = 0L

    suspend fun current(context: Context, force: Boolean = false): WeatherSnapshot =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!force && cached != null && now - cachedAtMs < 15 * 60_000L) {
                return@withContext cached!!
            }
            val (lat, lon, label) = resolveLocation(context)
            val url =
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code&temperature_unit=fahrenheit"
            val snapshot = runCatching {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    requestMethod = "GET"
                }
                conn.inputStream.bufferedReader().use { reader ->
                    val json = JSONObject(reader.readText())
                    val current = json.getJSONObject("current")
                    val temp = current.getDouble("temperature_2m").toInt()
                    val code = current.getInt("weather_code")
                    WeatherSnapshot(
                        tempF = temp,
                        condition = weatherCodeLabel(code),
                        locationLabel = label,
                        isLive = true,
                    )
                }.also { conn.disconnect() }
            }.getOrElse {
                cached ?: WeatherSnapshot(
                    tempF = 72,
                    condition = "Unavailable",
                    locationLabel = label,
                    isLive = false,
                )
            }
            cached = snapshot
            cachedAtMs = now
            snapshot
        }

    private fun resolveLocation(context: Context): Triple<Double, Double, String> {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val loc = runCatching {
            lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull()
        if (loc != null) {
            val label = runCatching {
                if (!Geocoder.isPresent()) return@runCatching null
                val geo = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                geo.getFromLocation(loc.latitude, loc.longitude, 1)
                    ?.firstOrNull()
                    ?.locality
            }.getOrNull() ?: "Near you"
            return Triple(loc.latitude, loc.longitude, label)
        }
        // Default: approximate continental US center
        return Triple(39.8283, -98.5795, "Local")
    }

    private fun weatherCodeLabel(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Mostly clear"
        3 -> "Cloudy"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Showers"
        95, 96, 99 -> "Thunder"
        else -> "Mixed"
    }
}
