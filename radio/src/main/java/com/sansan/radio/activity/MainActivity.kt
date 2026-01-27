package com.sansan.radio.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.sansan.radio.databinding.ActivityMainBinding
import com.sansan.radio.utils.hide
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bt.apply {

            setOnClickListener {
                // 最简单的协程使用
                lifecycleScope.launch {
                    val complexResult = withContext(Dispatchers.Default) {

                        while (true){

                            binding.radioScaleView.setFrequency(testFrequency)
                            if(g){
                                testFrequency += 0.1f
                                if(testFrequency > 108){
                                    g = false
                                }
                            } else {
                                testFrequency -= 0.1f
                                if(testFrequency < 87.5){
                                    g = true
                                }
                            }

                            delay(20)
                        }


                    }

                    text = "$complexResult"

                    hide()

                }
            }



        }


    }


}