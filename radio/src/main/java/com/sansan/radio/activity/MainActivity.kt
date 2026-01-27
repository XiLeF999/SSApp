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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bt.setOnClickListener {
            // 最简单的协程使用
            lifecycleScope.launch {
                val complexResult = withContext(Dispatchers.Default) {
                    // 模拟耗时计算
                    delay(2000)  // 1秒计算
                    2 + 2
                }

                binding.bt.text = "$complexResult"

                binding.bt.hide()
            }
        }


    }


}