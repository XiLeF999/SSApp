package com.sansan.radio.manager

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.fmradio.FmNative
import kotlinx.coroutines.*

class SSRadioController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    //初始化块
    init {

        controlRadio()

    }

    fun play(frequency: Float) {
        scope.launch {
            val isSuccess = withContext(Dispatchers.IO) {
                // 打开设备
                if (!FmNative.openDev()) {
                    Log.e("ww", "打开FM设备失败")
                    Toast.makeText(context,"打开FM设备失败", Toast.LENGTH_SHORT).show()
                    return@withContext false
                }

                // 上电
                if (!FmNative.powerUp(frequency)) {
                    Log.e("ww", "FM上电失败")
                    Toast.makeText(context,"FM上电失败", Toast.LENGTH_SHORT).show()
                    FmNative.closeDev()
                    return@withContext false
                }

                // 调谐到目标频率
                if (!FmNative.tune(frequency)) {
                    Log.e("ww", "调谐频率失败: ${frequency}MHz")
                    Toast.makeText(context,"调谐频率失败: ${frequency}MHz", Toast.LENGTH_SHORT).show()
                    FmNative.powerDown(0)
                    FmNative.closeDev()
                    return@withContext false
                }

                // 取消静音
                FmNative.setMute(false)

                return@withContext true
            }

            if (isSuccess) {
                Log.e("ww", "Radio播放成功:${frequency}MHz")
                Toast.makeText(context,"Radio播放成功:${frequency}MHz", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("ww", "Radio播放失败:${frequency}MHz")
                Toast.makeText(context,"Radio播放失败:${frequency}MHz", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun controlRadio() {

        SSRadioManager.addOnRadioControlListener(object : SSRadioManager.OnRadioControlListener {

            override fun autoScan() {

                scope.launch {
                    val stations = withContext(Dispatchers.IO) {

                        // 打开设备
                        if (!FmNative.openDev()) {
                            return@withContext shortArrayOf()
                        }

                        // 上电到起始频率
                        if (!FmNative.powerUp(87.5f)) {
                            FmNative.closeDev()
                            return@withContext shortArrayOf()
                        }

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

            override fun playStation(frequency: Float) {
                play(frequency)
            }

        })

    }






    fun release() {
        scope.cancel()
    }


}