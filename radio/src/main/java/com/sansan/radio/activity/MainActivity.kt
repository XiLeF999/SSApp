package com.sansan.radio.activity

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.sansan.radio.databinding.ActivityMainBinding
import com.android.fmradio.FmNative
import com.sansan.radio.utils.hide
import com.sansan.radio.utils.roundToOneDecimal
import com.sansan.radio.utils.setTransparentStatusBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {

    private lateinit var binding: ActivityMainBinding

    private var testFrequency = 87.5f
    private var g = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initRadioDev()





        binding.ivSearch.setOnClickListener {

            lifecycleScope.launch {  // 在主线程启动协程
                while (true) {
                    // 1. 执行UI更新（在主线程）
                    binding.viewRadioScale.setFrequency(testFrequency)

                    binding.tvFrequency.text = testFrequency.roundToOneDecimal().toString()

                    if(g){
                        testFrequency += 0.1f
                        if(testFrequency > 108.5){
                            g = false
                        }
                    } else {
                        testFrequency -= 0.1f
                        if(testFrequency < 87.5){
                            g = true
                        }
                    }

                    //delay() 挂起协程，不阻塞线程
                    delay(20)  // 挂起50ms，主线程可以处理其他消息
                }
            }

        }


    }



    private fun initRadioDev(){
        lifecycleScope.launch {
            val isSuccess = withContext(Dispatchers.IO) {
                // 执行耗时操作
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

            // 这里自动回到主线程，可以安全更新UI
            if (isSuccess) {

                Log.e("ww","Radio初始化成功");

            } else {

                Log.e("ww","Radio初始化失败");

            }
        }

    }




    // 测试调频
    fun testTune() {

        // 步骤1：打开设备
        val openResult = FmNative.openDev()
        if (!openResult) {
            Log.e("ww", "打开设备失败")
            return
        }

        // 步骤2：设备上电（必须！）
        // 需要指定一个初始频率，建议用87.5MHz
        val powerResult = FmNative.powerUp(87.5f)
        if (!powerResult) {
            Log.e("ww", "设备上电失败")
            FmNative.closeDev()  // 记得关闭设备
            return
        }

        // 步骤3：现在可以调用autoScan
        val stations = FmNative.autoScan()

        Log.e("ww", "扫描FM电台:$stations")


    }


}