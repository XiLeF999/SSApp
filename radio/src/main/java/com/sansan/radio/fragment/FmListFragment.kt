package com.sansan.radio.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.fmradio.FmNative
import com.sansan.radio.R
import com.sansan.radio.adapter.FmAdapter
import com.sansan.radio.bean.StationInfo
import com.sansan.radio.databinding.FragmentFmListBinding
import com.sansan.radio.manager.SSRadioManager

class FmListFragment : Fragment() {

    private lateinit var binding: FragmentFmListBinding

    private var mFmAdapter: FmAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFmListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 初始化视图和逻辑
        mFmAdapter = context?.let { FmAdapter(it) }
        binding.rvFmList.layoutManager = LinearLayoutManager(context)
        binding.rvFmList.adapter = mFmAdapter


        mFmAdapter?.setOnItemClickListener(object : FmAdapter.OnItemClickListener {
            override fun onItemClick(bean: StationInfo, position: Int) {


//                SSRadioManager.playStation(bean.frequency)


            }
        })

    }



    fun setRadioScanData(stations: List<StationInfo>) {
        mFmAdapter?.setData(stations)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // 清理资源
    }

}