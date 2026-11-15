package com.studyguard.app.core

import java.time.LocalDate
import java.time.ZoneId

object TimeUtils {
    fun startOfTodayMillis(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
}
