package com.sansan.radio.manager

import android.content.Context
import android.util.Log
import com.android.fmradio.FmNative
import kotlinx.coroutines.*

class SSRadioController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    //初始化块
    init {

        initRadio()

        controlRadio()

    }

    private fun initRadio() {
        scope.launch {
            val isSuccess = withContext(Dispatchers.IO) {
                val isOpenSuccess = FmNative.openDev()
                if (!isOpenSuccess) {
                    return@withContext false
                }

                val isPowerSuccess = FmNative.powerUp(87.5f)
                if (!isPowerSuccess) {
                    FmNative.closeDev()
                    return@withContext false
                }

                return@withContext true
            }

            if (isSuccess) {
                Log.e("ww", "Radio初始化成功")
            } else {
                Log.e("ww", "Radio初始化失败")
            }
        }
    }



    private fun controlRadio() {

        SSRadioManager.addOnRadioControlListener(object : SSRadioManager.OnRadioControlListener {

            override fun autoScan() {

                scope.launch {
                    val stations = withContext(Dispatchers.IO) {
                        FmNative.autoScan()?: shortArrayOf()  // 如果为 null 返回空数组
                    }

                    SSRadioManager.setAutoScan(stations)

                }

            }

            override fun stopScan() {

                scope.launch {
                    val stop = withContext(Dispatchers.IO) {
                        FmNative.stopScan()
                    }

                    SSRadioManager.setStopScan(stop)

                }
            }

        })

    }






    fun release() {
        scope.cancel()
    }


}