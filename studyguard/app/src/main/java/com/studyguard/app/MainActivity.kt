package com.studyguard.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.studyguard.app.service.MonitorService
import com.studyguard.app.ui.StudyGuardRoot
import com.studyguard.app.ui.theme.StudyGuardTheme
import com.studyguard.app.usage.UsageMonitor
import com.studyguard.app.vm.AppViewModel

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            StudyGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudyGuardRoot(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.refreshUsage()
        if (vm.currentSettings().monitoringEnabled && UsageMonitor.hasUsageAccess(this)) {
            MonitorService.start(this)
        }
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.getStringExtra(EXTRA_TAB)) {
            "learn" -> AppGraph.pendingTab.value = 1
            "parent" -> AppGraph.pendingTab.value = 3
        }
    }

    companion object {
        const val EXTRA_TAB = "extra_tab"
    }
}
