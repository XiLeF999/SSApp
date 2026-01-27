package com.sansan.radio

import android.app.Application
import android.content.Context

class SSApplication : Application() {

    companion object {
        @JvmStatic
        private var instance: SSApplication? = null

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