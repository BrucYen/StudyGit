package com.studyguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyguard.app.core.Constants
import com.studyguard.app.data.MonitorSettings
import com.studyguard.app.usage.InstalledApp
import com.studyguard.app.usage.UsageSummary
import com.studyguard.app.ui.components.PromoBanner
import com.studyguard.app.ui.components.SectionCard
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.Ink
import com.studyguard.app.ui.theme.InkSoft
import com.studyguard.app.ui.theme.Line
import com.studyguard.app.ui.theme.Mint
import com.studyguard.app.ui.theme.Sand
import com.studyguard.app.vm.AppViewModel

@Composable
fun GuardScreen(
    vm: AppViewModel,
    settings: MonitorSettings,
    usage: UsageSummary,
    installedApps: List<InstalledApp>,
    usageAccessGranted: Boolean
) {
    val usageMap = remember(usage) { usage.apps.associate { it.packageName to it.minutes } }
    val items = remember(installedApps, usage, settings.targetPackages) {
        val launcherMap = installedApps.associateBy { it.packageName }
        val monitored = settings.targetPackages.mapNotNull { pkg ->
            launcherMap[pkg] ?: when (pkg) {
                Constants.DOUYIN_PACKAGE -> InstalledApp(pkg, "抖音", null)
                Constants.DOUYIN_LITE_PACKAGE -> InstalledApp(pkg, "抖音极速版", null)
                else -> null
            }
        }
        val usedApps = usage.apps.mapNotNull { launcherMap[it.packageName] }
        (monitored + usedApps + installedApps)
            .distinctBy { it.packageName }
            .sortedWith(
                compareByDescending<InstalledApp> { it.packageName in settings.targetPackages }
                    .thenByDescending { usageMap[it.packageName] ?: 0L }
                    .thenBy { it.label }
            )
            .take(30)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("应用守护", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Text(
            "选择需要限制使用时长的应用，抖音默认已加入",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(
                label = "今日总时长",
                value = Constants.minutesText(usage.totalMinutes),
                modifier = Modifier.weight(1f)
            )
            SummaryTile(
                label = "目标应用",
                value = Constants.minutesText(usage.targetMinutes),
                modifier = Modifier.weight(1f),
                highlight = true
            )
        }

        Spacer(Modifier.height(16.dp))
        if (!usageAccessGranted) {
            PromoBanner("尚未开启「使用情况访问」权限，应用使用时长将显示为 0。请回到首页完成授权。")
            Spacer(Modifier.height(16.dp))
        }

        SectionCard(
            title = "监控应用（${settings.targetPackages.size} 个）",
            subtitle = "打开开关后，达到连续使用时长会弹出考试"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEach { app ->
                    val monitored = app.packageName in settings.targetPackages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(app)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = Ink
                            )
                            Text(
                                if (app.packageName == Constants.DOUYIN_PACKAGE) {
                                    "抖音 · 今日 ${Constants.minutesText(usageMap[app.packageName] ?: 0L)}"
                                } else {
                                    "今日 ${Constants.minutesText(usageMap[app.packageName] ?: 0L)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft
                            )
                        }
                        Switch(
                            checked = monitored,
                            onCheckedChange = { checked ->
                                vm.updateSettings { current ->
                                    val targets = if (checked) {
                                        current.targetPackages + app.packageName
                                    } else {
                                        current.targetPackages - app.packageName
                                    }
                                    current.copy(targetPackages = targets)
                                }
                                vm.refreshUsage()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Emerald,
                                uncheckedTrackColor = Color(0xFFE1E8E3)
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Sand, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Text(
                "提示：列表只展示常用应用，若有其他想限制的应用，可先在手机上正常使用一次，再回到这里刷新。",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7A5318)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AppIcon(app: InstalledApp) {
    if (app.icon != null) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Mint),
            contentAlignment = Alignment.Center
        ) {
            Text(
                app.label.take(1),
                style = MaterialTheme.typography.titleMedium,
                color = Emerald,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier
            .background(if (highlight) Mint else Color.White, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = InkSoft)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = if (highlight) Emerald else Ink
        )
    }
}
