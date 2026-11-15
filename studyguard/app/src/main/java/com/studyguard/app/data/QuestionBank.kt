package com.studyguard.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Serializable
data class QuizQuestion(
    val id: String,
    val subject: String,
    val gradeMin: Int,
    val gradeMax: Int,
    val stem: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String = ""
)

@Serializable
private data class QuestionBankFile(val questions: List<QuizQuestion> = emptyList())

class QuestionBank(context: Context) {

    private val appContext = context.applicationContext

    val questions: List<QuizQuestion> by lazy {
        runCatching {
            val raw = appContext.assets.open("questions.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            Json { ignoreUnknownKeys = true }
                .decodeFromString(QuestionBankFile.serializer(), raw)
                .questions
        }.getOrDefault(emptyList())
    }

    fun forGrade(grade: Int): List<QuizQuestion> =
        questions.filter { grade in it.gradeMin..it.gradeMax }

    fun forGradeAndSubject(grade: Int, subject: String): List<QuizQuestion> =
        questions.filter { it.subject == subject && grade in it.gradeMin..it.gradeMax }

    fun count(grade: Int, subject: String): Int = forGradeAndSubject(grade, subject).size

    fun randomForSubject(grade: Int, subject: String, count: Int): List<QuizQuestion> =
        forGradeAndSubject(grade, subject).shuffled().take(count)

    /**
     * 从多个科目中均衡抽题，用于跨科目弹窗考试。
     */
    fun pickExamQuestions(grade: Int, subjects: Set<String>, count: Int): List<QuizQuestion> {
        val pool = forGrade(grade).filter { it.subject in subjects }
        if (pool.isEmpty()) return emptyList()
        val bySubject = pool.groupBy { it.subject }.mapValues { it.value.shuffled() }
        val subjectOrder = bySubject.keys.shuffled()
        val picked = mutableListOf<QuizQuestion>()
        var round = 0
        while (picked.size < count) {
            var added = false
            for (subject in subjectOrder) {
                val list = bySubject[subject] ?: continue
                if (round < list.size) {
                    picked += list[round]
                    added = true
                    if (picked.size >= count) break
                }
            }
            if (!added) break
            round++
        }
        return picked.ifEmpty { pool.shuffled().take(count) }
    }

    fun randomQuestions(grade: Int, subject: String, count: Int): List<QuizQuestion> {
        val list = forGradeAndSubject(grade, subject).shuffled()
        if (list.isEmpty()) return emptyList()
        return List(count) { list[it % list.size] }
    }

    fun randomWordQuestions(count: Int = 8): List<QuizQuestion> =
        questions.filter { it.subject == "英语" }.shuffled().take(count)

    fun requiredCorrect(total: Int): Int = maxOf(1, (total * 2 + 2) / 3)

    fun shuffleOptions(question: QuizQuestion): Pair<List<String>, Int> {
        val indices = question.options.indices.shuffled(Random(System.nanoTime()))
        val options = indices.map { question.options[it] }
        val answer = indices.indexOf(question.answer)
        return options to answer
    }
}
