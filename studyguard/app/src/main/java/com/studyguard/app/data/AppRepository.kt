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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state: Flow<AppState> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> decode(prefs[stateKey]) }

    init {
        ensureDefaultAccount()
    }

    private fun ensureDefaultAccount() {
        scope.launch {
            val snapshot = state.first()
            if (snapshot.users.isNotEmpty()) return@launch
            val pinSalt = Crypto.newSalt()
            val settings = MonitorSettings(
                pinSalt = pinSalt,
                pinHash = Crypto.hash(Constants.DEFAULT_PARENT_PIN, pinSalt)
            )
            update { current ->
                if (current.users.isNotEmpty()) return@update current
                val salt = Crypto.newSalt()
                val account = UserAccount(
                    username = DEFAULT_USERNAME,
                    displayName = DEFAULT_DISPLAY_NAME,
                    salt = salt,
                    hash = Crypto.hash(DEFAULT_PASSWORD, salt)
                )
                current.copy(
                    users = listOf(account),
                    settings = current.settings + (DEFAULT_USERNAME to settings)
                )
            }
        }
    }

    companion object {
        const val DEFAULT_USERNAME = "parent"
        const val DEFAULT_PASSWORD = "123456"
        const val DEFAULT_DISPLAY_NAME = "默认家长"
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
            val snapshot = state.first()
            val account = snapshot.users.firstOrNull { it.username.equals(name, ignoreCase = true) }
                ?: return@withContext "用户名或密码错误"
            if (!Crypto.verify(password, account.salt, account.hash)) {
                return@withContext "用户名或密码错误"
            }
            update { current -> current.copy(sessionUsername = account.username) }
            null
        }

    suspend fun logout() {
        update { current -> current.copy(sessionUsername = null) }
    }

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
    }

    suspend fun updateSettings(transform: (MonitorSettings) -> MonitorSettings) {
        update { current ->
            val user = current.sessionUsername ?: return@update current
            val settings = current.settings[user] ?: MonitorSettings()
            current.copy(settings = current.settings + (user to transform(settings)))
        }
    }

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
}
