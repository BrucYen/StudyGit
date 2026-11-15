package com.studyguard.app.ui.learn

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyguard.app.core.Constants
import com.studyguard.app.ui.components.RoundedIcon
import com.studyguard.app.ui.components.SectionCard
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.Ink
import com.studyguard.app.ui.theme.InkSoft
import com.studyguard.app.ui.theme.Line
import com.studyguard.app.ui.theme.Mint
import com.studyguard.app.ui.theme.Sand
import kotlinx.coroutines.delay
import kotlin.random.Random

data class MathProblem(
    val text: String,
    val options: List<String>,
    val answerIndex: Int
)

private fun buildProblem(text: String, answer: Int): MathProblem {
    val options = linkedSetOf<Int>()
    options.add(answer)
    var guard = 0
    while (options.size < 4 && guard < 100) {
        guard++
        val delta = Random.nextInt(1, 7)
        val candidate = if (Random.nextBoolean()) answer + delta else (answer - delta).coerceAtLeast(0)
        if (candidate != answer) options.add(candidate)
    }
    val list = options.toList().shuffled()
    return MathProblem(text, list.map { it.toString() }, list.indexOf(answer))
}

fun generateMathProblem(grade: Int): MathProblem = when {
    grade <= 2 -> {
        val a = Random.nextInt(2, 20)
        val b = Random.nextInt(1, 20 - a + 1)
        if (Random.nextBoolean()) {
            buildProblem("$a + $b = ?", a + b)
        } else {
            val big = maxOf(a, b)
            val small = minOf(a, b)
            buildProblem("$big - $small = ?", big - small)
        }
    }

    grade <= 4 -> {
        val a = Random.nextInt(2, 10)
        val b = Random.nextInt(2, 10)
        if (Random.nextBoolean()) {
            buildProblem("$a × $b = ?", a * b)
        } else {
            val product = a * b
            buildProblem("$product ÷ $a = ?", b)
        }
    }

    grade <= 6 -> {
        val a = Random.nextInt(10, 30)
        val b = Random.nextInt(3, 10)
        buildProblem("$a × $b = ?", a * b)
    }

    else -> {
        val a = Random.nextInt(2, 10)
        val x = Random.nextInt(2, 13)
        val b = Random.nextInt(1, 21)
        buildProblem("解方程：${a}x + $b = ${a * x + b}，x = ?", x)
    }
}

@Composable
fun SpeedMathScreen(grade: Int, onExit: () -> Unit) {
    var question by remember { mutableStateOf(generateMathProblem(grade)) }
    var score by remember { mutableStateOf(0) }
    var streak by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(60) }
    var playing by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        while (timeLeft > 0) {
            delay(1000)
            timeLeft -= 1
        }
        playing = false
    }

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
            Column {
                Text("速算挑战", style = MaterialTheme.typography.titleLarge, color = Ink)
                Text(
                    "${Constants.gradeName(grade)} · 60 秒内答对越多越好",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("剩余时间", "$timeLeft 秒", Modifier.weight(1f))
            StatBox("得分", "$score", Modifier.weight(1f))
            StatBox("连对", "$streak", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        if (playing) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundedIcon(icon = Icons.Filled.Calculate, size = 38, iconSize = 20)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "选出正确答案",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkSoft
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    question.text,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                question.options.forEachIndexed { index, option ->
                    val isSelected = selected == index
                    val isCorrect = index == question.answerIndex
                    val revealed = selected != null
                    val background = when {
                        revealed && isCorrect -> Mint
                        revealed && isSelected -> Color(0xFFFFE7E7)
                        else -> Color(0xFFF3F6F4)
                    }
                    OutlinedButton(
                        onClick = {
                            if (selected != null) return@OutlinedButton
                            selected = index
                            if (isCorrect) {
                                score += 1
                                streak += 1
                            } else {
                                streak = 0
                            }
                            question = generateMathProblem(grade)
                            selected = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(52.dp)
                            .background(background, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                    ) {
                        Text(option, style = MaterialTheme.typography.titleLarge, color = Ink)
                    }
                }
            }
        } else {
            SectionCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("挑战结束", style = MaterialTheme.typography.headlineSmall, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "本轮答对 $score 题",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Emerald
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            score = 0
                            streak = 0
                            timeLeft = 60
                            question = generateMathProblem(grade)
                            selected = null
                            playing = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                    ) {
                        Text("再来一局")
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
                        Text("返回学习乐园", color = Ink)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Sand, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            fontWeight = FontWeight.Bold
        )
    }
}
