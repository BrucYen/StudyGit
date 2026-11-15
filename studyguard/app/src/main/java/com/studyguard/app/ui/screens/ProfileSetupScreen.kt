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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.studyguard.app.core.Constants
import com.studyguard.app.data.ChildProfile
import com.studyguard.app.data.RegionData
import com.studyguard.app.ui.components.ErrorText
import com.studyguard.app.ui.components.RoundedIcon
import com.studyguard.app.ui.components.SectionCard
import com.studyguard.app.ui.components.SelectorField
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.Ink
import com.studyguard.app.ui.theme.InkSoft
import com.studyguard.app.ui.theme.Line
import com.studyguard.app.ui.theme.Mint
import com.studyguard.app.ui.theme.Paper

@Composable
fun ProfileSetupScreen(
    initial: ChildProfile?,
    showBack: Boolean,
    onBack: () -> Unit,
    onSave: (ChildProfile) -> Unit
) {
    val base = initial ?: ChildProfile()
    var childName by remember(initial) { mutableStateOf(base.childName) }
    var province by remember(initial) { mutableStateOf(base.province) }
    var city by remember(initial) { mutableStateOf(base.city) }
    var grade by remember(initial) { mutableStateOf(base.grade) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Ink)
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                "孩子档案",
                style = MaterialTheme.typography.titleMedium,
                color = Ink
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundedIcon(icon = Icons.Filled.ChildCare, size = 52, iconSize = 28)
            Spacer(Modifier.size(14.dp))
            Column {
                Text(
                    "为学习内容匹配年级",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Ink
                )
                Text(
                    "地区与年级决定弹窗考试的题目难度",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionCard(title = "基本信息", subtitle = "这些信息只保存在本机") {
            OutlinedTextField(
                value = childName,
                onValueChange = {
                    childName = it
                    error = null
                },
                label = { Text("孩子昵称") },
                placeholder = { Text("例如：小雨") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald,
                    unfocusedBorderColor = Line,
                    focusedLabelColor = Emerald,
                    cursorColor = Emerald
                )
            )

            Spacer(Modifier.height(14.dp))
            SelectorField(
                label = "所在省份 / 地区",
                value = province,
                options = RegionData.provinces.map { it.name },
                onSelect = {
                    province = it
                    city = RegionData.defaultCity(it)
                }
            )

            Spacer(Modifier.height(14.dp))
            SelectorField(
                label = "所在城市",
                value = city,
                options = RegionData.citiesOf(province),
                onSelect = { city = it }
            )

            Spacer(Modifier.height(14.dp))
            SelectorField(
                label = "就读年级",
                value = Constants.gradeName(grade),
                options = (1..12).map { Constants.gradeName(it) },
                onSelect = { selected ->
                    grade = (1..12).first { Constants.gradeName(it) == selected }
                }
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Mint.copy(alpha = 0.55f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "当前题目范围：${Constants.gradeBand(grade)} · ${Constants.gradeName(grade)}",
                style = MaterialTheme.typography.bodySmall,
                color = Emerald
            )
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            ErrorText(error)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (childName.isBlank()) {
                    error = "请填写孩子昵称"
                } else {
                    onSave(
                        ChildProfile(
                            childName = childName.trim(),
                            province = province,
                            city = city,
                            grade = grade
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald)
        ) {
            Text("保存并开始守护", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        Spacer(Modifier.height(30.dp))
    }
}
