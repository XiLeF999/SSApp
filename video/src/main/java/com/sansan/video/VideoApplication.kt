package com.sansan.video

import android.app.Application
import android.content.Context

class VideoApplication : Application() {

    companion object {
        @JvmStatic
        private var instance: VideoApplication? = null

        @JvmStatic
        fun getAppContext(): Context? = instance?.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }
}