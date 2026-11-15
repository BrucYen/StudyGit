package com.studyguard.app.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.studyguard.app.ui.components.ErrorText
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.EmeraldDark
import com.studyguard.app.ui.theme.Ink
import com.studyguard.app.ui.theme.InkSoft
import com.studyguard.app.ui.theme.Line
import com.studyguard.app.vm.AppViewModel

@Composable
fun AuthScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    var isRegister by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val busy by vm.busy.collectAsState()
    val remoteError by vm.error.collectAsState()
    val error = localError ?: remoteError

    LaunchedEffect(isRegister) {
        vm.clearError()
        localError = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Emerald, EmeraldDark, Color(0xFF173F35))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .background(Color.White.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "学伴守护",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "把刷视频的时间，换成一次成长",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f)
            )
            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(26.dp)
                    ) {
                        AuthTab("登录", selected = !isRegister) { isRegister = false }
                        AuthTab("注册", selected = isRegister) { isRegister = true }
                    }
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            localError = null
                        },
                        label = { Text("用户名") },
                        placeholder = { Text("3-20 位字母、数字或下划线") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = fieldColors()
                    )

                    if (isRegister) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = {
                                nickname = it
                                localError = null
                            },
                            label = { Text("家长昵称") },
                            placeholder = { Text("例如：小明妈妈") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            colors = fieldColors()
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            localError = null
                        },
                        label = { Text("密码") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = fieldColors()
                    )

                    if (isRegister) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirm,
                            onValueChange = {
                                confirm = it
                                localError = null
                            },
                            label = { Text("确认密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            colors = fieldColors()
                        )
                    }

                    if (error != null) {
                        Spacer(Modifier.height(10.dp))
                        ErrorText(error)
                    }

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            localError = when {
                                username.isBlank() -> "请输入用户名"
                                password.isBlank() -> "请输入密码"
                                isRegister && nickname.isBlank() -> "请输入家长昵称"
                                isRegister && password.length < 6 -> "密码至少 6 位"
                                isRegister && password != confirm -> "两次输入的密码不一致"
                                else -> null
                            }
                            if (localError == null) {
                                if (isRegister) {
                                    vm.register(username, nickname, password)
                                } else {
                                    vm.login(username, password)
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (isRegister) "创建账号" else "登录",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    if (!isRegister) {
                        Spacer(Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "还没有账号？点击这里注册",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Emerald,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { isRegister = true }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        "账号与孩子档案仅保存在本机，不会上传服务器。",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                "适合 6-15 岁孩子的屏幕时间管理工具",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun AuthTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleLarge,
            color = if (selected) Ink else InkSoft,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .height(3.dp)
                .fillMaxWidth()
                .background(if (selected) Emerald else Color.Transparent)
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Emerald,
    unfocusedBorderColor = Line,
    focusedLabelColor = Emerald,
    cursorColor = Emerald
)
