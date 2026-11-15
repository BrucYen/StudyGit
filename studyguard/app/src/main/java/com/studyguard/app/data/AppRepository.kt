package com.studyguard.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.studyguard.app.core.Constants
import com.studyguard.app.core.Crypto
import com.studyguard.app.core.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "study_guard")

class AppRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val stateKey = stringPreferencesKey("app_state")
    private val tokenKey = stringPreferencesKey("auth_token")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state: Flow<AppState> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> decode(prefs[stateKey]) }

    init {
        // 启动时恢复 token
        scope.launch {
            val prefs = context.dataStore.data.first()
            ApiClient.token = prefs[tokenKey]
        }
    }

    private fun decode(raw: String?): AppState {
        if (raw.isNullOrBlank()) return AppState()
        return runCatching { json.decodeFromString<AppState>(raw) }.getOrDefault(AppState())
    }

    private suspend fun update(transform: (AppState) -> AppState) {
        context.dataStore.edit { prefs ->
            val current = decode(prefs[stateKey])
            prefs[stateKey] = json.encodeToString(AppState.serializer(), transform(current))
        }
    }

    private suspend fun saveToken(token: String?) {
        context.dataStore.edit { prefs ->
            if (token != null) prefs[tokenKey] = token else prefs.remove(tokenKey)
        }
        ApiClient.token = token
    }

    // ---- 认证 ----
    suspend fun register(username: String, displayName: String, password: String): String? =
        withContext(Dispatchers.IO) {
            val name = username.trim()
            val nickname = displayName.trim()
            when {
                !Regex("^[A-Za-z0-9_]{3,20}$").matches(name) ->
                    return@withContext "用户名需为 3-20 位字母、数字或下划线"
                nickname.isEmpty() -> return@withContext "请输入昵称"
                password.length < 6 -> return@withContext "密码至少 6 位"
            }
            // 优先走云端注册
            val resp = ApiClient.register(name, password, nickname)
            if (resp != null) {
                saveToken(resp.token)
                val salt = Crypto.newSalt()
                val account = UserAccount(
                    username = name,
                    displayName = nickname,
                    salt = salt,
                    hash = Crypto.hash(password, salt)
                )
                val pinSalt = Crypto.newSalt()
                val settings = MonitorSettings(
                    pinSalt = pinSalt,
                    pinHash = Crypto.hash(Constants.DEFAULT_PARENT_PIN, pinSalt)
                )
                update { current ->
                    current.copy(
                        users = (current.users.filterNot { it.username == name }) + account,
                        sessionUsername = name,
                        settings = current.settings + (name to settings)
                    )
                }
                return@withContext null
            }
            // 云端失败，回退本地注册
            val snapshot = state.first()
            if (snapshot.users.any { it.username.equals(name, ignoreCase = true) }) {
                return@withContext "该用户名已被注册"
            }
            val salt = Crypto.newSalt()
            val account = UserAccount(
                username = name,
                displayName = nickname,
                salt = salt,
                hash = Crypto.hash(password, salt)
            )
            val pinSalt = Crypto.newSalt()
            val settings = MonitorSettings(
                pinSalt = pinSalt,
                pinHash = Crypto.hash(Constants.DEFAULT_PARENT_PIN, pinSalt)
            )
            update { current ->
                current.copy(
                    users = current.users + account,
                    sessionUsername = name,
                    settings = current.settings + (name to settings)
                )
            }
            null
        }

    suspend fun login(username: String, password: String): String? =
        withContext(Dispatchers.IO) {
            val name = username.trim()
            if (name.isEmpty() || password.isEmpty()) return@withContext "请输入用户名和密码"

            // 优先走云端登录
            val resp = ApiClient.login(name, password)
            if (resp != null) {
                saveToken(resp.token)
                // 同步云端档案/设置到本地
                syncFromCloud(name, password)
                return@withContext null
            }

            // 云端失败，回退本地登录
            val snapshot = state.first()
            val account = snapshot.users.firstOrNull { it.username.equals(name, ignoreCase = true) }
                ?: return@withContext "用户名或密码错误"
            if (!Crypto.verify(password, account.salt, account.hash)) {
                return@withContext "用户名或密码错误"
            }
            update { current -> current.copy(sessionUsername = account.username) }
            null
        }

    // 登录成功后从云端拉取档案和设置
    private suspend fun syncFromCloud(username: String, password: String) {
        val profile = ApiClient.getProfile()
        val settingsRaw = ApiClient.getSettingsRaw()
        update { current ->
            var next = current.copy(sessionUsername = username)
            if (profile != null) {
                next = next.copy(profiles = next.profiles + (username to profile))
            }
            if (settingsRaw != null) {
                runCatching {
                    val remoteSettings = json.decodeFromString<ApiClient.SettingsResponse>(settingsRaw).settings
                    if (remoteSettings != null) {
                        val local = next.settings[username] ?: MonitorSettings()
                        val merged = mergeSettings(local, remoteSettings)
                        next = next.copy(settings = next.settings + (username to merged))
                    }
                }
            }
            // 确保有默认 PIN 设置
            val cur = next.settings[username] ?: MonitorSettings()
            if (cur.pinHash.isBlank()) {
                val salt = Crypto.newSalt()
                next = next.copy(
                    settings = next.settings + (username to cur.copy(
                        pinSalt = salt,
                        pinHash = Crypto.hash(Constants.DEFAULT_PARENT_PIN, salt)
                    ))
                )
            }
            // 确保本地也有用户记录（用于离线登录）
            if (next.users.none { it.username == username }) {
                val salt = Crypto.newSalt()
                next = next.copy(
                    users = next.users + UserAccount(
                        username = username,
                        displayName = username,
                        salt = salt,
                        hash = Crypto.hash(password, salt)
                    )
                )
            }
            next
        }
    }

    private fun mergeSettings(local: MonitorSettings, remote: Map<String, kotlinx.serialization.json.JsonElement>): MonitorSettings {
        return local.copy(
            monitoringEnabled = remote["monitoringEnabled"]?.toString()?.toBooleanStrictOrNull() ?: local.monitoringEnabled,
            dailyLimitMinutes = remote["dailyLimitMinutes"]?.toString()?.toIntOrNull() ?: local.dailyLimitMinutes,
            sessionLimitMinutes = remote["sessionLimitMinutes"]?.toString()?.toIntOrNull() ?: local.sessionLimitMinutes,
            quizQuestionCount = remote["quizQuestionCount"]?.toString()?.toIntOrNull() ?: local.quizQuestionCount,
            rewardMinutes = remote["rewardMinutes"]?.toString()?.toIntOrNull() ?: local.rewardMinutes,
            maxQuizRetries = remote["maxQuizRetries"]?.toString()?.toIntOrNull() ?: local.maxQuizRetries
        )
    }

    suspend fun logout() {
        scope.launch { ApiClient.logout() }
        saveToken(null)
        update { current -> current.copy(sessionUsername = null) }
    }

    // ---- 档案 ----
    suspend fun saveProfile(profile: ChildProfile) {
        update { current ->
            val user = current.sessionUsername ?: return@update current
            val existing = current.settings[user] ?: MonitorSettings()
            val settings = if (existing.pinHash.isBlank()) {
                val salt = Crypto.newSalt()
                existing.copy(
                    pinSalt = salt,
                    pinHash = Crypto.hash(Constants.DEFAULT_PARENT_PIN, salt)
                )
            } else {
                existing
            }
            current.copy(
                profiles = current.profiles + (user to profile),
                settings = current.settings + (user to settings)
            )
        }
        // 同步到云端
        scope.launch { ApiClient.saveProfile(profile) }
    }

    // ---- 设置 ----
    suspend fun updateSettings(transform: (MonitorSettings) -> MonitorSettings) {
        var updated: MonitorSettings? = null
        update { current ->
            val user = current.sessionUsername ?: return@update current
            val settings = current.settings[user] ?: MonitorSettings()
            updated = transform(settings)
            current.copy(settings = current.settings + (user to updated!!))
        }
        // 同步到云端（异步，不阻塞）
        updated?.let { s ->
            scope.launch {
                runCatching {
                    val jsonBody = json.encodeToString(MonitorSettings.serializer(), s)
                    ApiClient.saveSettings(jsonBody)
                }
            }
        }
    }

    // ---- PIN ----
    suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val settings = state.first().settings() ?: return@withContext false
        if (settings.pinHash.isBlank()) return@withContext pin == Constants.DEFAULT_PARENT_PIN
        Crypto.verify(pin, settings.pinSalt, settings.pinHash)
    }

    suspend fun changePin(newPin: String): String? = withContext(Dispatchers.IO) {
        if (!Regex("^\\d{4,6}$").matches(newPin)) return@withContext "PIN 需为 4-6 位数字"
        val salt = Crypto.newSalt()
        val hash = Crypto.hash(newPin, salt)
        updateSettings { it.copy(pinSalt = salt, pinHash = hash) }
        null
    }

    // ---- 学习记录 ----
    suspend fun recordQuizPass() {
        updateSettings {
            it.copy(
                quizPassCount = it.quizPassCount + 1,
                lastQuizAt = System.currentTimeMillis()
            )
        }
    }

    suspend fun addBonusMinutes(minutes: Int) {
        val today = TimeUtils.todayEpochDay()
        updateSettings {
            val base = if (it.bonusDateEpochDay == today) it.bonusMinutes else 0
            val rewardBase = if (it.rewardDateEpochDay == today) it.rewardCountToday else 0
            it.copy(
                bonusMinutes = base + minutes,
                bonusDateEpochDay = today,
                rewardCountToday = rewardBase + 1,
                rewardDateEpochDay = today
            )
        }
    }

    // ---- 错题本 ----
    suspend fun recordWrongQuestion(questionId: String, subject: String, stem: String) {
        scope.launch { ApiClient.addWrongQuestion(questionId, subject, stem) }
    }

    suspend fun removeWrongQuestion(questionId: String) {
        scope.launch { ApiClient.removeWrongQuestion(questionId) }
    }

    suspend fun getWrongQuestionIds(): Set<String> = withContext(Dispatchers.IO) {
        ApiClient.getWrongQuestions().map { it.questionId }.toSet()
    }
}
