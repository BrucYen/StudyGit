package com.studyguard.app.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyguard.app.AppGraph
import com.studyguard.app.core.Constants
import com.studyguard.app.data.QuizQuestion
import com.studyguard.app.service.OverlayMode
import com.studyguard.app.service.OverlayPayload
import com.studyguard.app.ui.components.RoundedIcon
import com.studyguard.app.ui.quiz.ExplanationCard
import com.studyguard.app.ui.quiz.QuestionOptions
import com.studyguard.app.ui.quiz.QuizProgress
import com.studyguard.app.ui.quiz.SubjectChip
import com.studyguard.app.ui.theme.Amber
import com.studyguard.app.ui.theme.Danger
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.Ink
import com.studyguard.app.ui.theme.InkSoft
import com.studyguard.app.ui.theme.Line
import com.studyguard.app.ui.theme.Mint
import com.studyguard.app.ui.theme.Sand

@Composable
fun OverlayQuizScreen(
    payload: OverlayPayload,
    onQuizPassed: () -> Unit,
    onParentUnlock: () -> Unit,
    onGoLearn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF21B2B24), Color(0xF20E1612))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "学伴守护 · 专注考试",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.14f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        payload.childName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    when (payload.mode) {
                        OverlayMode.QUIZ -> QuizOverlayContent(payload, onQuizPassed, onParentUnlock, onGoLearn)
                        OverlayMode.DAILY_LIMIT -> DailyLimitContent(payload, onGoLearn, onParentUnlock)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizOverlayContent(
    payload: OverlayPayload,
    onQuizPassed: () -> Unit,
    onParentUnlock: () -> Unit,
    onGoLearn: () -> Unit
) {
    val bank = AppGraph.questionBank
    var questions by remember { mutableStateOf(payload.questions) }
    var index by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var correctCount by remember { mutableStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var retries by remember { mutableStateOf(0) }

    val required = bank.requiredCorrect(questions.size.coerceAtLeast(1))
    val passed = correctCount >= required
    val retriesLeft = payload.settings.maxQuizRetries - retries
    val locked = !passed && retriesLeft <= 0

    if (!finished) {
        QuizProgress(current = index, total = questions.size)
        Spacer(Modifier.height(16.dp))
        val question = questions.getOrNull(index) ?: questions.firstOrNull()
        if (question == null) {
            Text("题库加载失败，请联系家长检查应用。", color = Ink)
            return
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectChip(question.subject)
            Spacer(Modifier.width(8.dp))
            Text(
                "答对 $required 题即可继续使用",
                style = MaterialTheme.typography.labelMedium,
                color = InkSoft
            )
            Spacer(Modifier.weight(1f))
            if (retries > 0) {
                Text(
                    "剩余重试 $retriesLeft 次",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (retriesLeft <= 1) Danger else InkSoft
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            question.stem,
            style = MaterialTheme.typography.titleLarge,
            color = Ink
        )
        Spacer(Modifier.height(16.dp))
        QuestionOptions(
            question = question,
            selected = selected,
            onSelect = { chosen ->
                selected = chosen
                if (chosen == question.answer) correctCount += 1
            }
        )
        if (selected != null) {
            Spacer(Modifier.height(14.dp))
            ExplanationCard(question, selected!!)
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                if (selected == null) return@Button
                if (index < questions.lastIndex) {
                    index += 1
                    selected = null
                } else {
                    finished = true
                }
            },
            enabled = selected != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald)
        ) {
            Text(
                if (index < questions.lastIndex) "下一题" else "查看结果",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "连续使用 ${payload.settings.sessionLimitMinutes} 分钟后需要完成 ${questions.size} 道${payload.subjects.joinToString("、")}题",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft
        )
    } else if (passed) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            RoundedIcon(
                icon = Icons.Filled.CheckCircle,
                tint = Emerald,
                background = Mint,
                size = 68,
                iconSize = 34
            )
            Spacer(Modifier.height(14.dp))
            Text("考试通过", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "答对 $correctCount / ${questions.size} 题",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "已获得 ${payload.settings.rewardMinutes} 分钟使用时间",
                style = MaterialTheme.typography.titleMedium,
                color = Emerald
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onQuizPassed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) {
                Text("继续使用", style = MaterialTheme.typography.titleMedium)
            }
        }
    } else if (locked) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            RoundedIcon(
                icon = Icons.Filled.Lock,
                tint = Danger,
                background = Sand,
                size = 68,
                iconSize = 34
            )
            Spacer(Modifier.height(14.dp))
            Text("重试次数已用完", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "本次答对 $correctCount / ${questions.size} 题，需要 $required 题",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "请去学习乐园巩固一下，或请家长解锁延长使用时间。",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onGoLearn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) {
                Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("去学习乐园", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onParentUnlock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = InkSoft, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("家长解锁", style = MaterialTheme.typography.titleMedium, color = Ink)
            }
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            RoundedIcon(
                icon = Icons.Filled.School,
                tint = Amber,
                background = Sand,
                size = 68,
                iconSize = 34
            )
            Spacer(Modifier.height(14.dp))
            Text("还差一点点", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "答对 $correctCount / ${questions.size} 题，需要 $required 题",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "还可以重试 $retriesLeft 次，把错题看一遍再来",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val newQuestions = bank.pickExamQuestions(
                        payload.grade, payload.subjects, payload.questionCount
                    )
                    questions = newQuestions.ifEmpty { payload.questions }
                    index = 0
                    selected = null
                    correctCount = 0
                    finished = false
                    retries += 1
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) {
                Text("再来一组", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun DailyLimitContent(
    payload: OverlayPayload,
    onGoLearn: () -> Unit,
    onParentUnlock: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        RoundedIcon(
            icon = Icons.Filled.Timer,
            tint = Amber,
            background = Sand,
            size = 68,
            iconSize = 34
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "今日使用时间已用完",
            style = MaterialTheme.typography.headlineSmall,
            color = Ink
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "今天在目标应用上已使用 ${Constants.minutesText(payload.targetMinutes)}",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Sand, RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("每日额度", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                Text(
                    "${payload.settings.effectiveDailyLimit()} 分钟",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("连续使用", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                Text(
                    "${payload.continuousMinutes} 分钟",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onGoLearn,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald)
        ) {
            Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("去学习乐园", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onParentUnlock,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Line)
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = InkSoft, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("家长解锁", style = MaterialTheme.typography.titleMedium, color = Ink)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "家长解锁后可增加今日使用时长；建议先陪孩子完成一次练习。",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft
        )
    }
}
