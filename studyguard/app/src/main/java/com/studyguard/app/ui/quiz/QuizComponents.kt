package com.studyguard.app.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyguard.app.data.QuizQuestion
import com.studyguard.app.ui.theme.Danger
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.Ink
import com.studyguard.app.ui.theme.InkSoft
import com.studyguard.app.ui.theme.Line
import com.studyguard.app.ui.theme.Mint
import com.studyguard.app.ui.theme.Sand

val OptionLetters = listOf("A", "B", "C", "D")

@Composable
fun QuizProgress(current: Int, total: Int, label: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label ?: "第 ${current + 1} / $total 题",
                style = MaterialTheme.typography.labelLarge,
                color = InkSoft
            )
            Text(
                "${((current + 1) * 100 / total.coerceAtLeast(1))}%",
                style = MaterialTheme.typography.labelMedium,
                color = Emerald
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (current + 1).toFloat() / total.coerceAtLeast(1) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
            color = Emerald,
            trackColor = Mint
        )
    }
}

@Composable
fun SubjectChip(subject: String) {
    Box(
        modifier = Modifier
            .background(Sand, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            subject,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8A5A17)
        )
    }
}

@Composable
fun QuestionOptions(
    question: QuizQuestion,
    selected: Int?,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        question.options.forEachIndexed { index, option ->
            val isSelected = selected == index
            val isCorrect = index == question.answer
            val revealed = selected != null
            val borderColor = when {
                revealed && isCorrect -> Emerald
                revealed && isSelected && !isCorrect -> Danger
                else -> Line
            }
            val background = when {
                revealed && isCorrect -> Mint.copy(alpha = 0.7f)
                revealed && isSelected && !isCorrect -> Danger.copy(alpha = 0.08f)
                else -> Color.White
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(16.dp))
                    .clickable(enabled = selected == null) { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            when {
                                revealed && isCorrect -> Emerald
                                revealed && isSelected -> Danger
                                else -> Mint
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        OptionLetters.getOrElse(index) { "?" },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (revealed && (isCorrect || isSelected)) Color.White else Emerald,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    option,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink
                )
            }
        }
    }
}

@Composable
fun ExplanationCard(question: QuizQuestion, selected: Int) {
    val correct = selected == question.answer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (correct) Mint.copy(alpha = 0.5f) else Danger.copy(alpha = 0.07f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            if (correct) "回答正确" else "正确答案：${question.options.getOrElse(question.answer) { "" }}",
            style = MaterialTheme.typography.titleSmall,
            color = if (correct) Emerald else Danger
        )
        if (question.explanation.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                question.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft
            )
        }
    }
}
