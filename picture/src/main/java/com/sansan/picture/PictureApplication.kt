package com.sansan.picture

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class PictureApplication : Application() {
    companion object {
        @JvmStatic private var instance: PictureApplication? = null
        @JvmStatic fun getAppContext(): Context? = instance?.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (resources.getBoolean(R.bool.is_ky1045)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
