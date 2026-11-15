package com.studyguard.app.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.studyguard.app.core.TimeUtils

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

data class AppUsage(
    val packageName: String,
    val label: String,
    val minutes: Long
)

data class UsageSummary(
    val totalMinutes: Long = 0,
    val targetMinutes: Long = 0,
    val apps: List<AppUsage> = emptyList(),
    val currentPackage: String? = null
)

object UsageMonitor {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isScreenOn(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return power.isInteractive
    }

    fun foregroundPackage(context: Context, lookbackMillis: Long = 60_000L): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(end - lookbackMillis, end) ?: return null
        val event = UsageEvents.Event()
        var pkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                pkg = event.packageName
            }
        }
        return pkg
    }

    /**
     * 统计从 since 到当前时刻各应用的前台使用时长（毫秒）。
     */
    fun usageSince(context: Context, since: Long): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        if (since >= end) return emptyMap()
        val events = usm.queryEvents(since, end) ?: return emptyMap()
        val event = UsageEvents.Event()
        val durations = HashMap<String, Long>()
        var currentPkg: String? = null
        var currentStart = since

        fun accumulate(until: Long) {
            val pkg = currentPkg ?: return
            val delta = (until - currentStart).coerceAtLeast(0L)
            if (delta > 0) durations[pkg] = (durations[pkg] ?: 0L) + delta
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    accumulate(event.timeStamp)
                    currentPkg = event.packageName
                    currentStart = event.timeStamp
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (currentPkg == event.packageName) {
                        accumulate(event.timeStamp)
                        currentPkg = null
                    }
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    accumulate(event.timeStamp)
                    currentPkg = null
                }
            }
        }
        if (currentPkg != null) accumulate(end)
        return durations
    }

    fun installedLauncherApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        val seen = HashSet<String>()
        val result = ArrayList<InstalledApp>()
        for (info in resolveInfos) {
            val appInfo = info.activityInfo?.applicationInfo ?: continue
            val pkg = appInfo.packageName
            if (pkg == context.packageName || !seen.add(pkg)) continue
            val label = runCatching { pm.getApplicationLabel(appInfo).toString() }
                .getOrDefault(pkg)
            val icon = runCatching {
                drawableToBitmap(pm.getApplicationIcon(appInfo))
            }.getOrNull()
            result += InstalledApp(pkg, label, icon)
        }
        return result.sortedBy { it.label }
    }

    fun usageSummary(
        context: Context,
        targetPackages: Set<String>,
        since: Long = TimeUtils.startOfTodayMillis()
    ): UsageSummary {
        val durations = usageSince(context, since)
        val launcher = installedLauncherApps(context).associateBy { it.packageName }
        val apps = durations.entries
            .filter { it.value >= 60_000L }
            .mapNotNull { (pkg, ms) ->
                val label = launcher[pkg]?.label ?: if (pkg in targetPackages) pkg else return@mapNotNull null
                AppUsage(pkg, label, ms / 60_000L)
            }
            .sortedByDescending { it.minutes }
        val targetMinutes = durations.entries
            .filter { it.key in targetPackages }
            .sumOf { it.value } / 60_000L
        return UsageSummary(
            totalMinutes = apps.sumOf { it.minutes },
            targetMinutes = targetMinutes,
            apps = apps,
            currentPackage = foregroundPackage(context)
        )
    }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): ImageBitmap? {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }
}
