package com.sansan.radio.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.sansan.radio.databinding.ActivityMainBinding
import com.android.fmradio.FmNative
import com.sansan.radio.R
import com.sansan.radio.manager.SSRadioManager
import com.sansan.radio.utils.hide
import com.sansan.radio.utils.roundToOneDecimal
import com.sansan.radio.utils.setTransparentStatusBar
import kotlinx.coroutines.*

class MainActivity : FragmentActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentFrequency = 87.5f
    private var isScanning = false
    private var animGoOrBack = true

    private var scanJob: Job? = null  // 保存协程引用

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransparentStatusBar()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivSearch.setOnClickListener(clickListener)
        changeRadio()

    }


    private val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.iv_search -> {

                if (isScanning) {

                    SSRadioManager.stopScan()

                } else {

                    binding.ivSearch.isSelected = true
                    isScanning = true

                    // 取消之前的扫描任务
                    scanJob?.cancel()

                    scanJob = lifecycleScope.launch(Dispatchers.Main) {  // 在主线程启动协程
                        while (isScanning && isActive) {
                            // 执行UI更新（在主线程）
                            binding.viewRadioScale.setFrequency(currentFrequency)

                            binding.tvFrequency.text =
                                currentFrequency.roundToOneDecimal().toString()

                            if (animGoOrBack) {
                                currentFrequency += 0.1f
                                if (currentFrequency > 108.5) {
                                    animGoOrBack = false
                                }
                            } else {
                                currentFrequency -= 0.1f
                                if (currentFrequency < 87.5) {
                                    animGoOrBack = true
                                }
                            }

                            //delay() 挂起协程，不阻塞线程
                            delay(20)  // 挂起50ms，主线程可以处理其他消息
                        }
                    }


                    SSRadioManager.autoScan()

                }


            }

            R.id.iv_play -> {

            }
            else -> {
                // 处理其他情况
            }
        }
    }


    private fun changeRadio() {

        SSRadioManager.addOnRadioChangeListener(object : SSRadioManager.OnRadioChangeListener {
            override fun setAutoScan(stations: ShortArray) {   //扫描结束
                binding.ivSearch.isSelected = false
                isScanning = false
                scanJob?.cancel()  // 取消之前的协程
                scanJob = null


            }

            override fun setStopScan(stop: Boolean) {
                if (stop) {   //主动停止扫描成功
                    binding.ivSearch.isSelected = false
                    isScanning = false
                    scanJob?.cancel()  // 取消之前的协程
                    scanJob = null

                    currentFrequency = 87.5f
                    binding.viewRadioScale.setFrequency(currentFrequency)
                    binding.tvFrequency.text = currentFrequency.roundToOneDecimal().toString()
                }

            }


        })

    }


    override fun onDestroy() {
        super.onDestroy()
        binding.ivSearch.isSelected = false
        isScanning = false
        scanJob?.cancel()
        scanJob = null
    }


}