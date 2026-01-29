package com.sansan.radio

import android.app.Application
import android.content.Context
import android.content.Intent
import com.sansan.radio.service.SSRadioService

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

        startService(Intent(this, SSRadioService::class.java))
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }
}