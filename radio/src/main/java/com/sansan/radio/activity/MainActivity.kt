package com.sansan.radio.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.sansan.radio.databinding.ActivityMainBinding
import com.android.fmradio.FmNative
import com.sansan.radio.R
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








    val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.iv_search -> {

            }

            R.id.iv_play -> {

            }
            else -> {
                // 处理其他情况
            }
        }
    }



}