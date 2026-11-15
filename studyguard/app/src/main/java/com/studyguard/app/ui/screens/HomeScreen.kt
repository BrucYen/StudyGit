package com.studyguard.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.studyguard.app.core.Constants
import com.studyguard.app.data.ChildProfile
import com.studyguard.app.data.MonitorSettings
import com.studyguard.app.usage.UsageMonitor
import com.studyguard.app.usage.UsageSummary
import com.studyguard.app.ui.components.ErrorText
import com.studyguard.app.ui.components.PermissionRow
import com.studyguard.app.ui.components.RoundedIcon
import com.studyguard.app.ui.components.SectionCard
import com.studyguard.app.ui.components.StatusPill
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.EmeraldDark
import com.studyguard.app.ui.theme.Ink
import com.studyguard.app.ui.theme.InkSoft
import com.studyguard.app.ui.theme.Line
import com.studyguard.app.ui.theme.Mint
import com.studyguard.app.ui.theme.Sand
import com.studyguard.app.vm.AppViewModel

@Composable
fun HomeScreen(
    vm: AppViewModel,
    userDisplayName: String,
    profile: ChildProfile,
    settings: MonitorSettings,
    usage: UsageSummary,
    onNavigateTab: (Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tick by remember { mutableStateOf(0) }
    var actionError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                tick += 1
                vm.refreshUsage()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            vm.refreshUsage()
        }
    }

    val usageGranted = remember(tick) { UsageMonitor.hasUsageAccess(context) }
    val overlayGranted = remember(tick) { UsageMonitor.hasOverlayPermission(context) }
    val notificationsGranted = remember(tick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { tick += 1 }

    val effectiveLimit = settings.effectiveDailyLimit()
    val remaining = (effectiveLimit - usage.targetMinutes).coerceAtLeast(0)
    val progress = (usage.targetMinutes.toFloat() / effectiveLimit.coerceAtLeast(1)).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Mint),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Emerald)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "你好，$userDisplayName",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink
                )
                Text(
                    "${profile.childName} · ${Constants.gradeName(profile.grade)} · ${profile.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft
                )
            }
            StatusPill(
                text = if (settings.monitoringEnabled) "守护中" else "未开启",
                active = settings.monitoringEnabled
            )
        }

        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(Emerald, EmeraldDark)))
                    .padding(20.dp)
            ) {
                Text(
                    "今日屏幕时间",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    Constants.minutesText(usage.totalMinutes),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroStat(
                        label = "目标应用",
                        value = Constants.minutesText(usage.targetMinutes),
                        modifier = Modifier.weight(1f)
                    )
                    HeroStat(
                        label = "每日额度",
                        value = "$effectiveLimit 分钟",
                        modifier = Modifier.weight(1f)
                    )
                    HeroStat(
                        label = "剩余",
                        value = "$remaining 分钟",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
                Spacer(Modifier.height(18.dp))
                if (settings.monitoringEnabled) {
                    OutlinedButton(
                        onClick = {
                            vm.setMonitoring(false)
                            actionError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Icon(
                            Icons.Filled.Pause,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("暂停守护", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = {
                            if (!usageGranted || !overlayGranted) {
                                actionError = "请先完成下方「使用情况访问」和「悬浮窗」授权"
                            } else {
                                actionError = null
                                vm.setMonitoring(true)
                                vm.refreshUsage()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Emerald
                        )
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("开启守护", style = MaterialTheme.typography.titleMedium)
                    }
                }
                ErrorText(actionError, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        if (!usageGranted || !overlayGranted || !notificationsGranted) {
            SectionCard(
                title = "还差几步即可守护",
                subtitle = "以下权限仅用于本机计时与弹窗，不会上传任何数据"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PermissionRow(
                        title = "使用情况访问",
                        description = "读取各应用使用时长，用于判断抖音等应用是否超时",
                        granted = usageGranted,
                        onOpen = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    )
                    PermissionRow(
                        title = "悬浮窗权限",
                        description = "超时后在其他应用上方弹出考试，答对才能继续使用",
                        granted = overlayGranted,
                        onOpen = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    )
                    PermissionRow(
                        title = "通知权限",
                        description = "显示守护运行状态，避免系统回收监测服务",
                        granted = notificationsGranted,
                        onOpen = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallStat(
                icon = Icons.Filled.School,
                label = "已通过考试",
                value = "${settings.quizPassCount} 次",
                modifier = Modifier.weight(1f)
            )
            SmallStat(
                icon = Icons.Filled.Timer,
                label = "今日奖励",
                value = "${settings.bonusForToday()} 分钟",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(title = "快捷入口") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionRow(
                    icon = Icons.Filled.School,
                    title = "去学习乐园",
                    subtitle = "速算、单词卡片与分科闯关",
                    onClick = { onNavigateTab(1) }
                )
                ActionRow(
                    icon = Icons.Filled.Apps,
                    title = "管理监控应用",
                    subtitle = "选择需要限制的娱乐应用",
                    onClick = { onNavigateTab(2) }
                )
                ActionRow(
                    icon = Icons.Filled.Lock,
                    title = "家长中心",
                    subtitle = "年级、科目、时长与 PIN 设置",
                    onClick = { onNavigateTab(3) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(title = "守护是怎么工作的") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StepRow("1", "开启权限后启动守护，应用在后台统计目标应用的使用时长。")
                StepRow("2", "连续使用达到设定分钟数，会在屏幕上弹出跨科目考试。")
                StepRow("3", "答对规定题数可获得额外使用时间；每日额度用完则需家长解锁。")
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Sand, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = null,
                tint = Color(0xFF8A5A17),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "建议在系统设置中将本应用加入电池优化白名单，避免后台被清理。",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7A5318)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White
        )
    }
}

@Composable
private fun SmallStat(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundedIcon(icon = icon, size = 38, iconSize = 20)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = InkSoft)
            Text(value, style = MaterialTheme.typography.titleMedium, color = Ink)
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundedIcon(icon = icon, size = 40, iconSize = 20)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSoft)
        }
        Text("进入", style = MaterialTheme.typography.labelMedium, color = Emerald)
    }
}

@Composable
private fun StepRow(index: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Mint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                index,
                style = MaterialTheme.typography.labelSmall,
                color = Emerald
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = InkSoft)
    }
}
