package com.studyguard.app.data

import com.studyguard.app.core.Constants
import com.studyguard.app.core.TimeUtils
import kotlinx.serialization.Serializable

@Serializable
data class UserAccount(
    val username: String,
    val displayName: String,
    val salt: String,
    val hash: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ChildProfile(
    val childName: String = "",
    val province: String = "广东省",
    val city: String = "深圳市",
    val grade: Int = 3
)

@Serializable
data class MonitorSettings(
    val monitoringEnabled: Boolean = false,
    val targetPackages: Set<String> = Constants.DEFAULT_TARGETS,
    val dailyLimitMinutes: Int = 40,
    val sessionLimitMinutes: Int = 15,
    val quizQuestionCount: Int = 3,
    val rewardMinutes: Int = 10,
    val maxQuizRetries: Int = 2,
    val subjects: Set<String> = Constants.SUBJECTS.toSet(),
    val pinSalt: String = "",
    val pinHash: String = "",
    val quizPassCount: Int = 0,
    val rewardCountToday: Int = 0,
    val rewardDateEpochDay: Long = 0,
    val bonusMinutes: Int = 0,
    val bonusDateEpochDay: Long = 0,
    val lastQuizAt: Long = 0
) {
    fun bonusForToday(): Int =
        if (bonusDateEpochDay == TimeUtils.todayEpochDay()) bonusMinutes else 0

    fun effectiveDailyLimit(): Int = dailyLimitMinutes + bonusForToday()
}

@Serializable
data class AppState(
    val users: List<UserAccount> = emptyList(),
    val sessionUsername: String? = null,
    val profiles: Map<String, ChildProfile> = emptyMap(),
    val settings: Map<String, MonitorSettings> = emptyMap()
) {
    fun user(): UserAccount? = users.firstOrNull { it.username == sessionUsername }
    fun profile(): ChildProfile? = sessionUsername?.let { profiles[it] }
    fun settings(): MonitorSettings? = sessionUsername?.let { settings[it] }
}
