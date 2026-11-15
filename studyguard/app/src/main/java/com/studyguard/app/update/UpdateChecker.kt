package com.studyguard.app.update

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false,
    val hasUpdate: Boolean = false
)

object UpdateChecker {

    private const val UPDATE_URL = "https://wytek.cloud/api/version"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkUpdate(context: Context): VersionInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val currentCode = PackageInfoCompat.getLongVersionCode(
                context.packageManager.getPackageInfo(context.packageName, 0)
            ).toInt()

            val url = java.net.URL("$UPDATE_URL?versionCode=$currentCode")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val info = json.decodeFromString<VersionInfo>(body)
            if (info.hasUpdate || info.versionCode > currentCode) info else null
        }.getOrNull()
    }
}
