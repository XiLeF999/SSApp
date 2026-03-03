package com.sansan.picture

import android.app.Application
import android.content.Context

class PictureApplication : Application() {

    companion object {
        @JvmStatic
        private var instance: PictureApplication? = null

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