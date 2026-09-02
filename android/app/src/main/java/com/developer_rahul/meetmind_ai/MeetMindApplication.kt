package com.developer_rahul.meetmind_ai

import android.app.Application
import com.developer_rahul.meetmind_ai.core.di.AppContainer

class MeetMindApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        lateinit var instance: MeetMindApplication
            private set
    }
}
