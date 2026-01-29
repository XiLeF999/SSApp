package com.sansan.radio.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.sansan.radio.manager.SSRadioController

class SSRadioService : Service() {

    private lateinit var ssRadioController: SSRadioController

    override fun onCreate() {
        super.onCreate()

        Log.e("ww","启动收音机服务RadioService");

        ssRadioController = SSRadioController(this)


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
    }
}