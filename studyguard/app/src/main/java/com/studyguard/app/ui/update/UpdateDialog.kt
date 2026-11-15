package com.studyguard.app.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyguard.app.update.VersionInfo
import com.studyguard.app.ui.theme.Emerald
import com.studyguard.app.ui.theme.Ink
import com.studyguard.app.ui.theme.InkSoft

@Composable
fun UpdateDialog(
    info: VersionInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!info.forceUpdate) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        containerColor = androidx.compose.ui.graphics.Color.White,
        title = {
            Text(
                text = "发现新版本 v${info.versionName}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = info.releaseNotes.ifBlank { "修复了一些问题，优化了用户体验。" },
                    fontSize = 14.sp,
                    color = InkSoft,
                    lineHeight = 22.sp
                )
                if (info.forceUpdate) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "本次为强制更新，请立即升级以继续使用。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "点击「立即更新」将在浏览器中下载新版本，下载完成后点击安装即可。",
                    fontSize = 12.sp,
                    color = InkSoft
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (info.forceUpdate) Arrangement.Center else Arrangement.End
            ) {
                if (!info.forceUpdate) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("稍后") }
                }
                Button(
                    onClick = { downloadApk(context, info.downloadUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                ) { Text("立即更新") }
            }
        }
    )
}

private fun downloadApk(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Toast.makeText(context, "正在浏览器中下载新版本...", Toast.LENGTH_LONG).show()
    }.onFailure {
        Toast.makeText(context, "无法打开下载链接，请手动访问 $url", Toast.LENGTH_LONG).show()
    }
}
