package com.sansan.radio.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.sansan.radio.R
import com.sansan.radio.bean.StationInfo
import com.sansan.radio.databinding.ActivityMainBinding
import com.sansan.radio.fragment.FavListFragment
import com.sansan.radio.fragment.FmListFragment
import com.sansan.radio.manager.SSRadioManager
import com.sansan.radio.service.SSRadioService
import com.sansan.radio.utils.roundToOneDecimal
import com.sansan.radio.utils.setTransparentStatusBar
import kotlinx.coroutines.*

class MainActivity : FragmentActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentFrequency = 87.5f
    private var isScanning = false
    private var animGoOrBack = true

    private var scanJob: Job? = null  // 保存协程引用

    private val fmFragment by lazy { FmListFragment() }
    private val favFragment by lazy { FavListFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransparentStatusBar()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startService(Intent(this, SSRadioService::class.java))

        binding.ivPlay.setOnClickListener(clickListener)
        binding.ivSearch.setOnClickListener(clickListener)
        binding.tvFmType.setOnClickListener(clickListener)
        binding.tvFavType.setOnClickListener(clickListener)

        binding.tvFmType.isSelected = true
        changeFragment(1)

        changeRadio()

    }


    private val clickListener = View.OnClickListener { view ->
        when (view.id) {

            R.id.iv_play -> {


            }

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

            R.id.tv_fm_type -> {
                changeFragment(1)
            }

            R.id.tv_fav_type -> {
                changeFragment(2)
            }

            else -> {
                // 处理其他情况
            }
        }
    }


    private fun changeRadio() {

        SSRadioManager.addOnRadioChangeListener(object : SSRadioManager.OnRadioChangeListener {
            override fun setAutoScan(stations: ShortArray) {   //扫描结束

                Log.e("ww", "扫描到 ${stations.size} 个电台")

                val stationList = mutableListOf<StationInfo>()
                stations.forEachIndexed { i, station ->
                    stationList.add(
                        StationInfo(
                            id = i,
                            frequency = station / 10f,
                            isFavorite = false
                        )
                    )
                }

                fmFragment.setRadioScanData(stationList)

                binding.ivSearch.isSelected = false
                isScanning = false
                scanJob?.cancel()  // 取消之前的协程
                scanJob = null

                currentFrequency = 87.5f
                binding.viewRadioScale.setFrequency(currentFrequency)
                binding.tvFrequency.text = currentFrequency.roundToOneDecimal().toString()

            }

            override fun setStopScan(stop: Boolean) {

                Log.e("ww", "停止扫描: $stop")

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

    /**
     * type:1-FM列表；2-搜藏喜欢的列表
     * 使用 add/hide/show 模式
     */
    private fun changeFragment(type: Int) {
        val transaction = supportFragmentManager.beginTransaction()

        // 隐藏所有Fragment
        if (fmFragment.isAdded) {
            transaction.hide(fmFragment)
        }
        if (favFragment.isAdded) {
            transaction.hide(favFragment)
        }

        when (type) {
            1 -> {
                binding.tvFmType.isSelected = true
                binding.tvFavType.isSelected = false

                if (fmFragment.isAdded) {
                    transaction.show(fmFragment)
                } else {
                    transaction.add(R.id.fragment_radio, fmFragment, "radio_fm")
                }
            }
            2 -> {
                binding.tvFmType.isSelected = false
                binding.tvFavType.isSelected = true
                if (favFragment.isAdded) {
                    transaction.show(favFragment)
                } else {
                    transaction.add(R.id.fragment_radio, favFragment, "radio_fav")
                }
            }
        }

        transaction.commitAllowingStateLoss()
    }


    override fun onDestroy() {
        super.onDestroy()
        binding.ivSearch.isSelected = false
        isScanning = false
        scanJob?.cancel()
        scanJob = null
    }


}