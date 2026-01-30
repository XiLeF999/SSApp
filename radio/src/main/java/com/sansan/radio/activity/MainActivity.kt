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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentFrequency = 87.5f
    private var animGoOrBack = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransparentStatusBar()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivSearch.setOnClickListener (clickListener)

    }








    private val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.iv_search -> {

                if(binding.ivSearch.isSelected){

                    binding.ivSearch.isSelected = false

                    currentFrequency = 87.5f
                    binding.viewRadioScale.setFrequency(currentFrequency)
                    binding.tvFrequency.text = currentFrequency.roundToOneDecimal().toString()

                } else {

                    binding.ivSearch.isSelected = true

                    lifecycleScope.launch {  // 在主线程启动协程
                        while (binding.ivSearch.isSelected) {
                            // 执行UI更新（在主线程）
                            binding.viewRadioScale.setFrequency(currentFrequency)

                            binding.tvFrequency.text = currentFrequency.roundToOneDecimal().toString()

                            if(animGoOrBack){
                                currentFrequency += 0.1f
                                if(currentFrequency > 108.5){
                                    animGoOrBack = false
                                }
                            } else {
                                currentFrequency -= 0.1f
                                if(currentFrequency < 87.5){
                                    animGoOrBack = true
                                }
                            }

                            //delay() 挂起协程，不阻塞线程
                            delay(20)  // 挂起50ms，主线程可以处理其他消息
                        }
                    }


//                    SSRadioManager.autoScan()

                }



            }

            R.id.iv_play -> {

            }
            else -> {
                // 处理其他情况
            }
        }
    }



}