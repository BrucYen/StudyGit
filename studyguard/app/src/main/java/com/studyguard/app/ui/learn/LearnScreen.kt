package com.studyguard.app.ui.learn

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyguard.app.AppGraph
import com.studyguard.app.core.Constants
import com.studyguard.app.data.ChildProfile
import com.studyguard.app.data.MonitorSettings
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.EmeraldDark
import com.studyguard.app.ui.theme.Ink
import com.studyguard.app.ui.theme.InkSoft
import com.studyguard.app.ui.theme.Line
import com.studyguard.app.ui.theme.Mint
import com.studyguard.app.ui.theme.Sand

private sealed interface LearnRoute {
    data object Hub : LearnRoute
    data class Quiz(val subject: String) : LearnRoute
    data object Speed : LearnRoute
    data object Words : LearnRoute
}

@Composable
fun LearnScreen(profile: ChildProfile, settings: MonitorSettings) {
    var route by remember { mutableStateOf<LearnRoute>(LearnRoute.Hub) }

    when (val current = route) {
        LearnRoute.Hub -> LearnHub(
            profile = profile,
            settings = settings,
            onOpenQuiz = { route = LearnRoute.Quiz(it) },
            onOpenSpeed = { route = LearnRoute.Speed },
            onOpenWords = { route = LearnRoute.Words }
        )

        is LearnRoute.Quiz -> PracticeQuizScreen(
            subject = current.subject,
            grade = profile.grade,
            subjects = settings.subjects,
            onExit = { route = LearnRoute.Hub }
        )

        LearnRoute.Speed -> SpeedMathScreen(
            grade = profile.grade,
            onExit = { route = LearnRoute.Hub }
        )

        LearnRoute.Words -> WordCardsScreen(
            grade = profile.grade,
            onExit = { route = LearnRoute.Hub }
        )
    }
}

@Composable
private fun LearnHub(
    profile: ChildProfile,
    settings: MonitorSettings,
    onOpenQuiz: (String) -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenWords: () -> Unit
) {
    val bank = AppGraph.questionBank
    val totalQuestions = Constants.SUBJECTS.sumOf { bank.count(profile.grade, it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("学习乐园", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Text(
            "${profile.childName} · ${Constants.gradeName(profile.grade)} · ${profile.province}${profile.city}",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft
        )
        Spacer(Modifier.height(16.dp))

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
                    "把刷视频的时间\n换成一次成长",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "本年级题库共 $totalQuestions 道 · 已通过考试 ${settings.quizPassCount} 次",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onOpenQuiz("综合") },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Emerald
                    )
                ) {
                    Icon(
                        Icons.Filled.School,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("开始综合闯关", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("互动学习模块", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModuleCard(
                icon = Icons.Filled.Calculate,
                title = "速算挑战",
                subtitle = "60 秒计时",
                modifier = Modifier.weight(1f),
                onClick = onOpenSpeed
            )
            ModuleCard(
                icon = Icons.Filled.Translate,
                title = "单词闪卡",
                subtitle = "点击记单词",
                modifier = Modifier.weight(1f),
                onClick = onOpenWords
            )
            ModuleCard(
                icon = Icons.Filled.MenuBook,
                title = "答题闯关",
                subtitle = "分科练习",
                modifier = Modifier.weight(1f),
                onClick = { onOpenQuiz("综合") }
            )
        }

        Spacer(Modifier.height(18.dp))
        Text("按科目练习", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(10.dp))

        val subjects = Constants.SUBJECTS
        subjects.chunked(2).forEach { rowSubjects ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowSubjects.forEach { subject ->
                    SubjectCard(
                        subject = subject,
                        count = bank.count(profile.grade, subject),
                        enabled = subject in settings.subjects,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenQuiz(subject) }
                    )
                }
                if (rowSubjects.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Sand, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text(
                "小提示：弹窗考试会从「家长中心」勾选的科目中抽题，答对后可以获得额外使用时间。",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7A5318)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ModuleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Mint, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Emerald, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSoft)
        }
    }
}

@Composable
private fun SubjectCard(
    subject: String,
    count: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subject,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) Ink else InkSoft
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "$count 题",
                    style = MaterialTheme.typography.labelMedium,
                    color = Emerald
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (enabled) "开始练习" else "家长未勾选该科目",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft
            )
        }
    }
}
