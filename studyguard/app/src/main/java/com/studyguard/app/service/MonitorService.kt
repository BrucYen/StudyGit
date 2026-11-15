package com.studyguard.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.studyguard.app.AppGraph
import com.studyguard.app.MainActivity
import com.studyguard.app.R
import com.studyguard.app.core.Constants
import com.studyguard.app.core.TimeUtils
import com.studyguard.app.data.MonitorSettings
import com.studyguard.app.data.QuizQuestion
import com.studyguard.app.usage.UsageMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loopJob: Job? = null

    private var sessionPackage: String? = null
    private var sessionStart: Long = 0L
    private var cooldownUntil: Long = 0L

    private var cachedTargetMinutes: Long = 0L
    private var lastUsageRefresh: Long = 0L
    private var cachedDay: Long = -1L
    private var lastNotificationText: String = ""

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("正在监测使用时长"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (loopJob == null) {
            loopJob = scope.launch { monitorLoop() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        loopJob?.cancel()
        scope.cancel()
        OverlayQuizController.dismiss()
        super.onDestroy()
    }

    private suspend fun monitorLoop() {
        while (scope.isActive) {
            delay(TICK_MILLIS)
            runCatching { tick() }
        }
    }

    private suspend fun tick() {
        val snapshot = AppGraph.repository.state.first()
        val settings = snapshot.settings() ?: return
        val profile = snapshot.profile() ?: return
        if (!settings.monitoringEnabled) return
        if (OverlayQuizController.isShowing) return
        if (!UsageMonitor.hasUsageAccess(this) || !UsageMonitor.hasOverlayPermission(this)) return
        if (!UsageMonitor.isScreenOn(this)) {
            sessionPackage = null
            return
        }

        val now = System.currentTimeMillis()
        val foreground = UsageMonitor.foregroundPackage(this)
        if (foreground != sessionPackage) {
            sessionPackage = foreground
            sessionStart = now
        }
        if (foreground == null || foreground == packageName) return
        if (foreground !in settings.targetPackages) return

        refreshCachedUsage(settings, now)

        val continuousMinutes = ((now - sessionStart) / 60_000L).toInt()
        updateNotification("目标应用今日 ${Constants.minutesText(cachedTargetMinutes)} · 连续使用 $continuousMinutes 分钟")

        if (now < cooldownUntil) return

        val dailyLimit = settings.effectiveDailyLimit()

        if (cachedTargetMinutes >= dailyLimit) {
            showOverlay(
                settings = settings,
                profile = profile,
                mode = OverlayMode.DAILY_LIMIT,
                questions = emptyList(),
                continuousMinutes = continuousMinutes
            )
        } else if (continuousMinutes >= settings.sessionLimitMinutes) {
            val questions = AppGraph.questionBank.pickExamQuestions(
                profile.grade, settings.subjects, settings.quizQuestionCount
            )
            if (questions.isEmpty()) {
                cooldownUntil = now + 60_000L
                return
            }
            showOverlay(
                settings = settings,
                profile = profile,
                mode = OverlayMode.QUIZ,
                questions = questions,
                continuousMinutes = continuousMinutes
            )
        }
    }

    private suspend fun refreshCachedUsage(settings: MonitorSettings, now: Long) {
        val today = TimeUtils.todayEpochDay()
        if (cachedDay == today && now - lastUsageRefresh < 30_000L) return
        val durations = UsageMonitor.usageSince(this, TimeUtils.startOfTodayMillis())
        cachedTargetMinutes = durations.entries
            .filter { it.key in settings.targetPackages }
            .sumOf { it.value } / 60_000L
        cachedDay = today
        lastUsageRefresh = now
    }

    private fun showOverlay(
        settings: MonitorSettings,
        profile: com.studyguard.app.data.ChildProfile,
        mode: OverlayMode,
        questions: List<QuizQuestion>,
        continuousMinutes: Int
    ) {
        val payload = OverlayPayload(
            mode = mode,
            childName = profile.childName.ifBlank { "同学" },
            grade = profile.grade,
            subjects = settings.subjects,
            questionCount = settings.quizQuestionCount,
            settings = settings,
            questions = questions,
            continuousMinutes = continuousMinutes,
            targetMinutes = cachedTargetMinutes
        )
        OverlayQuizController.show(
            context = this,
            payload = payload,
            onQuizPassed = {
                scope.launch {
                    val now = System.currentTimeMillis()
                    cooldownUntil = now + settings.rewardMinutes * 60_000L
                    sessionStart = now
                    OverlayQuizController.dismiss()
                    AppGraph.repository.recordQuizPass()
                }
            },
            onParentUnlock = {
                cooldownUntil = System.currentTimeMillis() + 2 * 60_000L
                OverlayQuizController.dismiss()
                openParentCenter()
            },
            onGoLearn = {
                cooldownUntil = System.currentTimeMillis() + 60_000L
                OverlayQuizController.dismiss()
                openLearn()
            }
        )
    }

    private fun openLearn() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_TAB, "learn")
        }
        runCatching { startActivity(intent) }
    }

    private fun openParentCenter() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_TAB, "parent")
        }
        runCatching { startActivity(intent) }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_text)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_guard)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        if (text == lastNotificationText) return
        lastNotificationText = text
        runCatching {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(text))
        }
    }

    companion object {
        const val ACTION_START = "com.studyguard.app.action.START_MONITOR"
        const val ACTION_STOP = "com.studyguard.app.action.STOP_MONITOR"
        private const val CHANNEL_ID = "study_guard_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val TICK_MILLIS = 5_000L

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java).setAction(ACTION_START)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, MonitorService::class.java)) }
        }
    }
}
