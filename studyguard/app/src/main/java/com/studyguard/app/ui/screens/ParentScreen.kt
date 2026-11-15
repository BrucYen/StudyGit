package com.studyguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studyguard.app.core.Constants
import com.studyguard.app.data.ChildProfile
import com.studyguard.app.data.MonitorSettings
import com.studyguard.app.ui.components.DotDivider
import com.studyguard.app.ui.components.ErrorText
import com.studyguard.app.ui.components.KeyValueRow
import com.studyguard.app.ui.components.RoundedIcon
import com.studyguard.app.ui.components.SectionCard
import com.studyguard.app.ui.components.StepperRow
import com.studyguard.app.ui.theme.Danger
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.Ink
import com.studyguard.app.ui.theme.InkSoft
import com.studyguard.app.ui.theme.Line
import com.studyguard.app.ui.theme.Mint
import com.studyguard.app.ui.theme.Sand
import com.studyguard.app.vm.AppViewModel

@Composable
fun ParentScreen(
    vm: AppViewModel,
    userDisplayName: String,
    profile: ChildProfile,
    settings: MonitorSettings,
    overlayGranted: Boolean,
    onNavigateTab: (Int) -> Unit
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var editingProfile by rememberSaveable { mutableStateOf(false) }

    if (editingProfile) {
        ProfileSetupScreen(
            initial = profile,
            showBack = true,
            onBack = { editingProfile = false },
            onSave = {
                vm.saveProfile(it)
                editingProfile = false
            }
        )
        return
    }

    if (!unlocked) {
        PinGate(
            vm = vm,
            onUnlock = { unlocked = true },
            onBack = { onNavigateTab(0) }
        )
        return
    }

    var showPinDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("家长中心", style = MaterialTheme.typography.headlineMedium, color = Ink)
                Text(
                    "$userDisplayName · 已验证家长 PIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft
                )
            }
            RoundedIcon(icon = Icons.Filled.Lock, size = 40, iconSize = 20)
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(title = "孩子档案", subtitle = "决定考试题目难度与地区信息") {
            KeyValueRow("孩子昵称", profile.childName)
            Spacer(Modifier.height(8.dp))
            KeyValueRow("所在地区", "${profile.province} ${profile.city}")
            Spacer(Modifier.height(8.dp))
            KeyValueRow(
                "就读年级",
                "${Constants.gradeName(profile.grade)}（${Constants.gradeBand(profile.grade)}）"
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { editingProfile = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Text("编辑档案", color = Ink)
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "使用规则", subtitle = "修改后立即生效") {
            StepperRow(
                title = "每日使用上限",
                subtitle = "目标应用一天可用总时长",
                valueText = "${settings.dailyLimitMinutes} 分钟",
                onDecrease = {
                    vm.updateSettings {
                        it.copy(dailyLimitMinutes = (it.dailyLimitMinutes - 10).coerceAtLeast(10))
                    }
                },
                onIncrease = {
                    vm.updateSettings {
                        it.copy(dailyLimitMinutes = (it.dailyLimitMinutes + 10).coerceAtMost(180))
                    }
                },
                canDecrease = settings.dailyLimitMinutes > 10,
                canIncrease = settings.dailyLimitMinutes < 180
            )
            Spacer(Modifier.height(6.dp))
            StepperRow(
                title = "连续使用触发考试",
                subtitle = "连续使用多久后弹出考试",
                valueText = "${settings.sessionLimitMinutes} 分钟",
                onDecrease = {
                    vm.updateSettings {
                        it.copy(sessionLimitMinutes = (it.sessionLimitMinutes - 5).coerceAtLeast(5))
                    }
                },
                onIncrease = {
                    vm.updateSettings {
                        it.copy(sessionLimitMinutes = (it.sessionLimitMinutes + 5).coerceAtMost(60))
                    }
                },
                canDecrease = settings.sessionLimitMinutes > 5,
                canIncrease = settings.sessionLimitMinutes < 60
            )
            Spacer(Modifier.height(6.dp))
            StepperRow(
                title = "通过考试奖励时长",
                subtitle = "答对后可以继续使用的时间",
                valueText = "${settings.rewardMinutes} 分钟",
                onDecrease = {
                    vm.updateSettings {
                        it.copy(rewardMinutes = (it.rewardMinutes - 5).coerceAtLeast(5))
                    }
                },
                onIncrease = {
                    vm.updateSettings {
                        it.copy(rewardMinutes = (it.rewardMinutes + 5).coerceAtMost(30))
                    }
                },
                canDecrease = settings.rewardMinutes > 5,
                canIncrease = settings.rewardMinutes < 30
            )
            Spacer(Modifier.height(6.dp))
            StepperRow(
                title = "每次考试题数",
                subtitle = "答对约 2/3 的题目即可通过",
                valueText = "${settings.quizQuestionCount} 题",
                onDecrease = {
                    vm.updateSettings {
                        it.copy(quizQuestionCount = (it.quizQuestionCount - 1).coerceAtLeast(1))
                    }
                },
                onIncrease = {
                    vm.updateSettings {
                        it.copy(quizQuestionCount = (it.quizQuestionCount + 1).coerceAtMost(5))
                    }
                },
                canDecrease = settings.quizQuestionCount > 1,
                canIncrease = settings.quizQuestionCount < 5
            )
            Spacer(Modifier.height(6.dp))
            StepperRow(
                title = "未通过最大重试次数",
                subtitle = "用完后只能家长解锁或去学习",
                valueText = "${settings.maxQuizRetries} 次",
                onDecrease = {
                    vm.updateSettings {
                        it.copy(maxQuizRetries = (it.maxQuizRetries - 1).coerceAtLeast(0))
                    }
                },
                onIncrease = {
                    vm.updateSettings {
                        it.copy(maxQuizRetries = (it.maxQuizRetries + 1).coerceAtMost(5))
                    }
                },
                canDecrease = settings.maxQuizRetries > 0,
                canIncrease = settings.maxQuizRetries < 5
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "考试科目", subtitle = "弹窗考试会从勾选的科目中抽题") {
            Constants.SUBJECTS.chunked(2).forEach { rowSubjects ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowSubjects.forEach { subject ->
                        val selected = subject in settings.subjects
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) settings.subjects - subject else settings.subjects + subject
                                if (next.isNotEmpty()) {
                                    vm.updateSettings { it.copy(subjects = next) }
                                }
                            },
                            label = { Text(subject) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Mint,
                                selectedLabelColor = Emerald
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                "至少保留一个科目。当前已选 ${settings.subjects.size} 个。",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "今日额度", subtitle = "答题通过或家长奖励会临时增加额度") {
            KeyValueRow("每日上限（含奖励）", "${settings.effectiveDailyLimit()} 分钟")
            Spacer(Modifier.height(8.dp))
            KeyValueRow(
                "今日奖励额度",
                "+${settings.bonusForToday()} 分钟",
                valueColor = Emerald
            )
            Spacer(Modifier.height(8.dp))
            KeyValueRow("通过考试次数", "${settings.quizPassCount} 次")
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { vm.addBonusMinutes(settings.rewardMinutes) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) {
                Text("今日延长 ${settings.rewardMinutes} 分钟")
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "测试与安全", subtitle = "用于确认弹窗与答题流程是否正常") {
            OutlinedButton(
                onClick = { vm.previewOverlay() },
                enabled = overlayGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Icon(
                    Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = if (overlayGranted) Emerald else InkSoft,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (overlayGranted) "预览弹窗考试" else "请先开启悬浮窗权限",
                    color = if (overlayGranted) Ink else InkSoft
                )
            }
            if (!overlayGranted) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "回到首页完成悬浮窗授权后即可预览。",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft
                )
            }
            Spacer(Modifier.height(14.dp))
            DotDivider()
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { showPinDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = InkSoft,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("修改家长 PIN", color = Ink)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "初始 PIN 为 1234，建议修改为只有家长知道的安全数字。",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "关于学伴守护") {
            Text(
                "版本 1.0.0 · 数据仅保存在本机\n考试题库按小学低/中/高年级与中学分段，数学、语文、英语、科学共 96 道。",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft
            )
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { vm.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.4f))
        ) {
            Text("退出登录", color = Danger)
        }
        Spacer(Modifier.height(26.dp))
    }

    if (showPinDialog) {
        ChangePinDialog(
            onDismiss = { showPinDialog = false },
            onChange = { pin, onResult -> vm.changePin(pin, onResult) }
        )
    }
}

@Composable
private fun PinGate(
    vm: AppViewModel,
    onUnlock: () -> Unit,
    onBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        RoundedIcon(icon = Icons.Filled.Lock, size = 72, iconSize = 34)
        Spacer(Modifier.height(16.dp))
        Text("家长中心", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "请输入 4-6 位家长 PIN",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                    pin = it
                    error = null
                }
            },
            label = { Text("家长 PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald,
                unfocusedBorderColor = Line,
                focusedLabelColor = Emerald,
                cursorColor = Emerald
            )
        )
        ErrorText(error, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (pin.length < 4) {
                    error = "PIN 至少 4 位"
                } else {
                    vm.verifyPin(pin) { ok ->
                        if (ok) onUnlock() else error = "PIN 不正确，请重试"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald)
        ) {
            Text("进入家长中心", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "初始 PIN 为 1234，登录后可在家长中心修改。",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) {
            Text("返回首页", color = Emerald)
        }
    }
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onChange: (String, (String?) -> Unit) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = { Text("修改家长 PIN", color = Ink) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                            pin = it
                            error = null
                        }
                    },
                    label = { Text("新的 4-6 位 PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald,
                        unfocusedBorderColor = Line
                    )
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = {
                        if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                            confirm = it
                            error = null
                        }
                    },
                    label = { Text("再次输入") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald,
                        unfocusedBorderColor = Line
                    )
                )
                ErrorText(error, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        pin.length < 4 -> error = "PIN 至少 4 位"
                        pin != confirm -> error = "两次输入不一致"
                        else -> onChange(pin) { result ->
                            if (result == null) onDismiss() else error = result
                        }
                    }
                }
            ) {
                Text("保存", color = Emerald)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = InkSoft)
            }
        }
    )
}
