package com.studyguard.app.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyguard.app.AppGraph
import com.studyguard.app.core.Constants
import com.studyguard.app.data.AppState
import com.studyguard.app.data.ChildProfile
import com.studyguard.app.data.MonitorSettings
import com.studyguard.app.service.MonitorService
import com.studyguard.app.service.OverlayMode
import com.studyguard.app.service.OverlayPayload
import com.studyguard.app.service.OverlayQuizController
import com.studyguard.app.usage.InstalledApp
import com.studyguard.app.usage.UsageMonitor
import com.studyguard.app.usage.UsageSummary
import com.studyguard.app.update.UpdateChecker
import com.studyguard.app.update.VersionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppGraph.repository

    val state: StateFlow<AppState?> = repo.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val usage = MutableStateFlow(UsageSummary())
    val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val updateInfo = MutableStateFlow<VersionInfo?>(null)
    val updateChecking = MutableStateFlow(false)

    init {
        refreshInstalledApps()
        refreshUsage()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            installedApps.value = withContext(Dispatchers.IO) {
                UsageMonitor.installedLauncherApps(getApplication())
            }
        }
    }

    fun refreshUsage() {
        viewModelScope.launch {
            val snapshot = state.value ?: repo.state.first()
            val settings = snapshot.settings() ?: MonitorSettings()
            usage.value = withContext(Dispatchers.IO) {
                UsageMonitor.usageSummary(getApplication(), settings.targetPackages)
            }
        }
    }

    fun clearError() {
        error.value = null
    }

    fun register(username: String, displayName: String, password: String) {
        busy.value = true
        viewModelScope.launch {
            val result = repo.register(username, displayName, password)
            error.value = result
            busy.value = false
        }
    }

    fun login(username: String, password: String) {
        busy.value = true
        viewModelScope.launch {
            val result = repo.login(username, password)
            error.value = result
            busy.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            MonitorService.stop(getApplication())
            repo.logout()
        }
    }

    fun saveProfile(profile: ChildProfile) {
        viewModelScope.launch { repo.saveProfile(profile) }
    }

    fun updateSettings(transform: (MonitorSettings) -> MonitorSettings) {
        viewModelScope.launch { repo.updateSettings(transform) }
    }

    fun setMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            repo.updateSettings { it.copy(monitoringEnabled = enabled) }
            val context = getApplication<Application>()
            if (enabled) {
                MonitorService.start(context)
            } else {
                MonitorService.stop(context)
            }
        }
    }

    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(repo.verifyPin(pin))
        }
    }

    fun changePin(newPin: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(repo.changePin(newPin))
        }
    }

    fun recordQuizPass() {
        viewModelScope.launch { repo.recordQuizPass() }
    }

    fun addBonusMinutes(minutes: Int) {
        viewModelScope.launch { repo.addBonusMinutes(minutes) }
    }

    fun previewOverlay() {
        val snapshot = state.value ?: return
        val settings = snapshot.settings() ?: return
        val profile = snapshot.profile() ?: return
        val questions = AppGraph.questionBank.pickExamQuestions(
            profile.grade, settings.subjects, settings.quizQuestionCount
        )
        if (questions.isEmpty()) return
        val payload = OverlayPayload(
            mode = OverlayMode.QUIZ,
            childName = profile.childName.ifBlank { "同学" },
            grade = profile.grade,
            subjects = settings.subjects,
            questionCount = settings.quizQuestionCount,
            settings = settings,
            questions = questions,
            continuousMinutes = settings.sessionLimitMinutes,
            targetMinutes = 0
        )
        OverlayQuizController.show(
            context = getApplication(),
            payload = payload,
            onQuizPassed = { OverlayQuizController.dismiss() },
            onParentUnlock = { OverlayQuizController.dismiss() },
            onGoLearn = {
                OverlayQuizController.dismiss()
                AppGraph.pendingTab.value = 1
            }
        )
    }

    fun currentSettings(): MonitorSettings = state.value?.settings() ?: MonitorSettings()

    fun currentProfile(): ChildProfile? = state.value?.profile()

    fun currentTargets(): Set<String> = currentSettings().targetPackages.ifEmpty { Constants.DEFAULT_TARGETS }

    fun checkUpdate() {
        viewModelScope.launch {
            updateChecking.value = true
            val info = UpdateChecker.checkUpdate(getApplication())
            updateChecking.value = false
            if (info != null) {
                updateInfo.value = info
            } else {
                error.value = "已是最新版本"
                kotlinx.coroutines.delay(2000)
                error.value = null
            }
        }
    }
}
