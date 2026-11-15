package com.studyguard.app.core

object Constants {
    const val DOUYIN_PACKAGE = "com.ss.android.ugc.aweme"
    const val DOUYIN_LITE_PACKAGE = "com.ss.android.ugc.aweme.lite"

    val SUBJECTS = listOf("数学", "语文", "英语", "科学")
    val DEFAULT_TARGETS = setOf(DOUYIN_PACKAGE, DOUYIN_LITE_PACKAGE)

    const val DEFAULT_PARENT_PIN = "1234"

    fun gradeName(grade: Int): String = when (grade) {
        1 -> "一年级"
        2 -> "二年级"
        3 -> "三年级"
        4 -> "四年级"
        5 -> "五年级"
        6 -> "六年级"
        7 -> "初一"
        8 -> "初二"
        9 -> "初三"
        10 -> "高一"
        11 -> "高二"
        12 -> "高三"
        else -> "三年级"
    }

    fun gradeBand(grade: Int): String = when (grade) {
        in 1..2 -> "小学低年级"
        in 3..4 -> "小学中年级"
        in 5..6 -> "小学高年级"
        in 7..9 -> "初中"
        else -> "高中"
    }

    fun minutesText(minutes: Long): String {
        if (minutes <= 0L) return "0 分钟"
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h <= 0 -> "$m 分钟"
            m == 0L -> "$h 小时"
            else -> "$h 小时 $m 分钟"
        }
    }

    fun subjectIconName(subject: String): String = when (subject) {
        "数学" -> "calculate"
        "语文" -> "menu_book"
        "英语" -> "translate"
        else -> "science"
    }
}
