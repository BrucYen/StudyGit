package com.studyguard.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.studyguard.app.AppGraph
import com.studyguard.app.data.ChildProfile
import com.studyguard.app.data.MonitorSettings
import com.studyguard.app.ui.screens.AuthScreen
import com.studyguard.app.ui.screens.GuardScreen
import com.studyguard.app.ui.screens.HomeScreen
import com.studyguard.app.ui.screens.ParentScreen
import com.studyguard.app.ui.screens.ProfileSetupScreen
import com.studyguard.app.ui.learn.LearnScreen
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.InkSoft
import com.studyguard.app.ui.theme.Mint
import com.studyguard.app.ui.theme.Paper
import com.studyguard.app.ui.update.UpdateDialog
import com.studyguard.app.update.UpdateChecker
import com.studyguard.app.update.VersionInfo
import com.studyguard.app.usage.InstalledApp
import com.studyguard.app.usage.UsageMonitor
import com.studyguard.app.usage.UsageSummary
import com.studyguard.app.vm.AppViewModel

@Composable
fun StudyGuardRoot(vm: AppViewModel) {
    val state by vm.state.collectAsState()

    when {
        state == null -> LoadingScreen()

        state?.sessionUsername == null -> AuthScreen(vm)

        state?.profile() == null -> ProfileSetupScreen(
            initial = null,
            showBack = false,
            onBack = {},
            onSave = { vm.saveProfile(it) }
        )

        else -> {
            val snapshot = state ?: return
            val profile = snapshot.profile() ?: return
            val settings = snapshot.settings() ?: MonitorSettings()
            MainScaffold(
                vm = vm,
                displayName = snapshot.user()?.displayName ?: "家长",
                profile = profile,
                settings = settings
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Emerald, modifier = Modifier.padding(8.dp))
    }
}

private data class TabItem(val label: String, val icon: ImageVector)

@Composable
private fun MainScaffold(
    vm: AppViewModel,
    displayName: String,
    profile: ChildProfile,
    settings: MonitorSettings
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val usage by vm.usage.collectAsState()
    val installedApps by vm.installedApps.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableStateOf(0) }
    var updateInfo by remember { mutableStateOf<VersionInfo?>(null) }

    // 启动时检查更新 + 拉取最新题库
    LaunchedEffect(Unit) {
        val info = UpdateChecker.checkUpdate(context)
        if (info != null) updateInfo = info
        // 后台拉取最新题库，失败不影响使用（回退到本地缓存/内置题库）
        AppGraph.questionBank.fetchFromServer()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeTick += 1
                vm.refreshUsage()
                vm.refreshInstalledApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        AppGraph.pendingTab.collect { tab ->
            if (tab != null) {
                selectedTab = tab
                AppGraph.pendingTab.value = null
            }
        }
    }

    val tabs = listOf(
        TabItem("首页", Icons.Filled.Home),
        TabItem("学习", Icons.Filled.School),
        TabItem("应用", Icons.Filled.Apps),
        TabItem("家长", Icons.Filled.Lock)
    )

    val usageAccessGranted = remember(resumeTick) { UsageMonitor.hasUsageAccess(context) }
    val overlayGranted = remember(resumeTick) { UsageMonitor.hasOverlayPermission(context) }

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald,
                            selectedTextColor = Emerald,
                            indicatorColor = Mint,
                            unselectedIconColor = InkSoft,
                            unselectedTextColor = InkSoft
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    vm = vm,
                    userDisplayName = displayName,
                    profile = profile,
                    settings = settings,
                    usage = usage,
                    onNavigateTab = { selectedTab = it }
                )

                1 -> LearnScreen(profile = profile, settings = settings)

                2 -> GuardScreen(
                    vm = vm,
                    settings = settings,
                    usage = usage,
                    installedApps = installedApps,
                    usageAccessGranted = usageAccessGranted
                )

                else -> ParentScreen(
                    vm = vm,
                    userDisplayName = displayName,
                    profile = profile,
                    settings = settings,
                    overlayGranted = overlayGranted,
                    onNavigateTab = { selectedTab = it }
                )
            }
        }

        updateInfo?.let { info ->
            UpdateDialog(info = info) { updateInfo = null }
        }
    }
}
