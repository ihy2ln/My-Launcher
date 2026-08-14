package com.homelauncher.app.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RecentAppsRepository {
    fun hasUsageAccess(context: Context): Boolean {
        val app = context.packageName
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        )
        // Usage access is a separate setting; probe the service.
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val start = end - 60_000L
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            stats != null && (stats.isNotEmpty() || canQuery(usm))
        } catch (_: Exception) {
            false
        } || app.isNotBlank() && runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, end - 86_400_000L, end)?.isNotEmpty() == true
        }.getOrDefault(false)
    }

    private fun canQuery(usm: UsageStatsManager): Boolean = true

    suspend fun recentPackageNames(context: Context, limit: Int = 8): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                    ?: return@withContext emptyList()
                val end = System.currentTimeMillis()
                val start = end - 3L * 86_400_000L
                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
                    ?: return@withContext emptyList()
                stats
                    .filter { it.packageName != context.packageName && it.lastTimeUsed > 0L }
                    .sortedByDescending { it.lastTimeUsed }
                    .map { it.packageName }
                    .distinct()
                    .take(limit)
            } catch (_: Exception) {
                emptyList()
            }
        }
}
