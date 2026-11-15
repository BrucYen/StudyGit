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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyguard.app.core.Constants
import com.studyguard.app.ui.components.RoundedIcon
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.EmeraldDark
import com.studyguard.app.ui.theme.Ink
import com.studyguard.app.ui.theme.InkSoft
import com.studyguard.app.ui.theme.Line
import com.studyguard.app.ui.theme.Mint

private data class Word(val english: String, val chinese: String)

private val BasicWords = listOf(
    Word("apple", "苹果"), Word("banana", "香蕉"), Word("cat", "猫"),
    Word("dog", "狗"), Word("book", "书"), Word("water", "水"),
    Word("sun", "太阳"), Word("moon", "月亮"), Word("red", "红色"),
    Word("blue", "蓝色"), Word("happy", "开心的"), Word("school", "学校")
)

private val MiddleWords = listOf(
    Word("teacher", "老师"), Word("student", "学生"), Word("friend", "朋友"),
    Word("family", "家庭"), Word("weather", "天气"), Word("holiday", "假期"),
    Word("question", "问题"), Word("answer", "答案"), Word("beautiful", "美丽的"),
    Word("delicious", "美味的"), Word("library", "图书馆"), Word("science", "科学")
)

private val UpperWords = listOf(
    Word("environment", "环境"), Word("knowledge", "知识"), Word("exercise", "锻炼"),
    Word("festival", "节日"), Word("invention", "发明"), Word("courage", "勇气"),
    Word("protect", "保护"), Word("improve", "提高"), Word("discover", "发现"),
    Word("important", "重要的"), Word("different", "不同的"), Word("remember", "记得")
)

private val AdvancedWords = listOf(
    Word("achievement", "成就"), Word("opportunity", "机会"), Word("challenge", "挑战"),
    Word("communicate", "交流"), Word("experience", "经历"), Word("responsible", "负责的"),
    Word("technology", "科技"), Word("tradition", "传统"), Word("encourage", "鼓励"),
    Word("accomplish", "完成"), Word("appreciate", "感激"), Word("confident", "自信的")
)

private fun wordsFor(grade: Int): List<Word> = when {
    grade <= 2 -> BasicWords
    grade <= 4 -> MiddleWords
    grade <= 6 -> UpperWords
    else -> AdvancedWords
}

@Composable
fun WordCardsScreen(grade: Int, onExit: () -> Unit) {
    val source = remember(grade) { wordsFor(grade) }
    var words by remember(grade) { mutableStateOf(source.shuffled()) }
    var index by remember(grade) { mutableStateOf(0) }
    var revealed by remember(grade) { mutableStateOf(false) }
    val word = words[index]

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
                Text("单词闪卡", style = MaterialTheme.typography.titleLarge, color = Ink)
                Text(
                    "${Constants.gradeName(grade)} · 点击卡片查看释义",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "${index + 1} / ${words.size}",
            style = MaterialTheme.typography.labelLarge,
            color = InkSoft
        )
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.linearGradient(listOf(Emerald, EmeraldDark)),
                    RoundedCornerShape(28.dp)
                )
                .clickable { revealed = !revealed },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RoundedIcon(
                    icon = Icons.Filled.Translate,
                    tint = Emerald,
                    background = Color.White,
                    size = 48,
                    iconSize = 24
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    word.english,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(14.dp))
                if (revealed) {
                    Text(
                        word.chinese,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                } else {
                    Text(
                        "轻触卡片显示中文",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    index = (index - 1 + words.size) % words.size
                    revealed = false
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Text("上一个", color = Ink)
            }
            Button(
                onClick = {
                    index = (index + 1) % words.size
                    revealed = false
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) {
                Text("下一个")
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                words = source.shuffled()
                index = 0
                revealed = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Line)
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = InkSoft,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text("打乱顺序", color = InkSoft)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Mint.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text(
                "小提示：先读英文，再猜中文，最后点开卡片核对，记忆更牢。",
                style = MaterialTheme.typography.bodySmall,
                color = Emerald
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
