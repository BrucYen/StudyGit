package com.studyguard.app.ui.learn

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studyguard.app.AppGraph
import com.studyguard.app.core.Constants
import com.studyguard.app.ui.components.RoundedIcon
import com.studyguard.app.ui.components.SectionCard
import com.studyguard.app.ui.quiz.ExplanationCard
import com.studyguard.app.ui.quiz.QuestionOptions
import com.studyguard.app.ui.quiz.QuizProgress
import com.studyguard.app.ui.quiz.SubjectChip
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.Ink
import com.studyguard.app.ui.theme.InkSoft
import com.studyguard.app.ui.theme.Line
import com.studyguard.app.ui.theme.Mint

@Composable
fun PracticeQuizScreen(
    subject: String,
    grade: Int,
    subjects: Set<String> = emptySet(),
    onExit: () -> Unit
) {
    val bank = AppGraph.questionBank
    var token by remember { mutableStateOf(0) }
    val questions = remember(subject, grade, token, subjects) {
        if (subject == "综合") {
            bank.pickExamQuestions(grade, subjects.ifEmpty { Constants.SUBJECTS.toSet() }, 6)
        } else {
            bank.randomForSubject(grade, subject, 6)
        }
    }
    var index by remember(subject, token) { mutableStateOf(0) }
    var selected by remember(subject, token) { mutableStateOf<Int?>(null) }
    var correctCount by remember(subject, token) { mutableStateOf(0) }
    var finished by remember(subject, token) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Ink)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (subject == "综合") "综合闯关" else "$subject · 互动练习",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink
                )
                Text(
                    "${Constants.gradeName(grade)} · 共 ${questions.size} 题",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (questions.isEmpty()) {
            SectionCard {
                Text("该科目暂时没有对应年级的题目。", color = Ink)
            }
            return@Column
        }

        if (!finished) {
            val question = questions[index]
            QuizProgress(current = index, total = questions.size)
            Spacer(Modifier.height(16.dp))
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SubjectChip(question.subject)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "已答对 $correctCount 题",
                        style = MaterialTheme.typography.labelMedium,
                        color = Emerald
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(question.stem, style = MaterialTheme.typography.titleLarge, color = Ink)
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
            }
            Spacer(Modifier.height(16.dp))
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
                    if (index < questions.lastIndex) "下一题" else "查看成绩",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            SectionCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RoundedIcon(
                        icon = Icons.Filled.CheckCircle,
                        size = 66,
                        iconSize = 34,
                        tint = Emerald,
                        background = Mint
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("本轮完成", style = MaterialTheme.typography.headlineSmall, color = Ink)
                    Text(
                        "答对 $correctCount / ${questions.size} 题",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSoft
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            token += 1
                            index = 0
                            selected = null
                            correctCount = 0
                            finished = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                    ) {
                        Text("再练一组")
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                    ) {
                        Icon(
                            Icons.Filled.School,
                            contentDescription = null,
                            tint = InkSoft,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("返回学习乐园", color = Ink)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
