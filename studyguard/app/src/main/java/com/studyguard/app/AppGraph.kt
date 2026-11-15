package com.studyguard.app

import android.content.Context
import com.studyguard.app.data.AppRepository
import com.studyguard.app.data.QuestionBank
import kotlinx.coroutines.flow.MutableStateFlow

object AppGraph {
    lateinit var repository: AppRepository
        private set
    lateinit var questionBank: QuestionBank
        private set

    /** 请求底部导航切换：0 首页，1 学习，2 应用，3 家长 */
    val pendingTab = MutableStateFlow<Int?>(null)

    fun init(context: Context) {
        repository = AppRepository(context.applicationContext)
        questionBank = QuestionBank(context.applicationContext)
    }
}
