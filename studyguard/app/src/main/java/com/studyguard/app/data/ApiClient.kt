package com.studyguard.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    const val BASE_URL = "https://wytek.cloud"
    private val json = Json { ignoreUnknownKeys = true }

    var token: String? = null

    // ---- 数据模型 ----
    @Serializable
    data class AuthResponse(val token: String, val username: String, val displayName: String)

    @Serializable
    data class ProfileResponse(val profile: ChildProfile?)

    @Serializable
    data class SettingsResponse(val settings: Map<String, kotlinx.serialization.json.JsonElement>?)

    @Serializable
    data class WrongQuestion(
        val questionId: String,
        val subject: String = "",
        val stem: String = "",
        val wrongCount: Int = 1,
        val lastWrongAt: Long = 0,
        val mastered: Boolean = false
    )

    @Serializable
    data class WrongQuestionsResponse(val questions: List<WrongQuestion> = emptyList())

    @Serializable
    data class StudyRecord(
        val date: String = "",
        val subject: String = "",
        val correct: Int = 0,
        val total: Int = 0,
        val createdAt: Long = 0
    )

    @Serializable
    data class StudyRecordsResponse(val records: List<StudyRecord> = emptyList())

    // ---- 通用请求 ----
    private suspend fun request(
        method: String,
        path: String,
        body: String? = null,
        auth: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("$BASE_URL$path").openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Content-Type", "application/json")
            if (auth && token != null) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.bufferedWriter().use { it.write(body) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code !in 200..299) return@runCatching null
            response
        }.getOrNull()
    }

    // ---- 认证 ----
    suspend fun register(username: String, password: String, displayName: String): AuthResponse? {
        val body = """{"username":"$username","password":"$password","displayName":"$displayName"}"""
        return request("POST", "/api/auth/register", body, auth = false)?.let {
            runCatching { json.decodeFromString<AuthResponse>(it) }.getOrNull()
        }
    }

    suspend fun login(username: String, password: String): AuthResponse? {
        val body = """{"username":"$username","password":"$password"}"""
        return request("POST", "/api/auth/login", body, auth = false)?.let {
            runCatching { json.decodeFromString<AuthResponse>(it) }.getOrNull()
        }
    }

    suspend fun logout(): Boolean {
        return request("POST", "/api/auth/logout") != null
    }

    // ---- 档案 ----
    suspend fun getProfile(): ChildProfile? {
        return request("GET", "/api/profile")?.let {
            runCatching { json.decodeFromString<ProfileResponse>(it).profile }.getOrNull()
        }
    }

    suspend fun saveProfile(profile: ChildProfile): Boolean {
        val body = json.encodeToString(ChildProfile.serializer(), profile)
        return request("PUT", "/api/profile", body) != null
    }

    // ---- 设置 ----
    suspend fun getSettingsRaw(): String? = request("GET", "/api/settings")

    suspend fun saveSettings(settingsJson: String): Boolean {
        return request("PUT", "/api/settings", settingsJson) != null
    }

    // ---- 错题本 ----
    suspend fun getWrongQuestions(): List<WrongQuestion> {
        return request("GET", "/api/wrong-questions")?.let {
            runCatching { json.decodeFromString<WrongQuestionsResponse>(it).questions }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    suspend fun addWrongQuestion(questionId: String, subject: String, stem: String): Boolean {
        val body = """{"questionId":"$questionId","subject":"$subject","stem":"$stem"}"""
        return request("POST", "/api/wrong-questions", body) != null
    }

    suspend fun removeWrongQuestion(questionId: String): Boolean {
        return request("DELETE", "/api/wrong-questions/$questionId") != null
    }

    // ---- 学习记录 ----
    suspend fun addStudyRecord(date: String, subject: String, correct: Int, total: Int): Boolean {
        val body = """{"date":"$date","subject":"$subject","correct":$correct,"total":$total}"""
        return request("POST", "/api/study-record", body) != null
    }

    suspend fun getStudyRecords(limit: Int = 50): List<StudyRecord> {
        return request("GET", "/api/study-records?limit=$limit")?.let {
            runCatching { json.decodeFromString<StudyRecordsResponse>(it).records }.getOrDefault(emptyList())
        } ?: emptyList()
    }
}
