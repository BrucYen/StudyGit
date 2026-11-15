package com.studyguard.app

import android.app.Application

class StudyGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
