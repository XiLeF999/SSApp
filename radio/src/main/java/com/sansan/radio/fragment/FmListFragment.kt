package com.sansan.radio.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.sansan.radio.adapter.FmAdapter
import com.sansan.radio.bean.StationInfo
import com.sansan.radio.databinding.FragmentFmListBinding
import com.sansan.radio.fm.FmStation

class FmListFragment : Fragment() {

    private lateinit var binding: FragmentFmListBinding

    private var mFmAdapter: FmAdapter? = null

    private var onFmListFragmentListener: OnFmListFragmentListener? = null

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

        updateStationList(FmStation.getCurrentStation(context))


        mFmAdapter?.setOnItemClickListener(object : FmAdapter.OnItemClickListener {
            override fun onItemClick(bean: StationInfo, position: Int) {

                onFmListFragmentListener?.powerUpAsync(bean.station)


            }
        })

    }



    fun updateStationList(currentStation: Int){

        val stationInfoList = FmStation.getSearchStation(context)
        stationInfoList.firstOrNull { it.station == currentStation }?.let { it.isSelect = true }

        mFmAdapter?.setData(stationInfoList, onFmListFragmentListener?.isPlaying() ?: true)
    }


    fun updatePlayAnim(isPlayAnim: Boolean){
        mFmAdapter?.setPlayAnim(isPlayAnim)
    }



    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateStationList(FmStation.getCurrentStation(context))
        }
    }


    fun setOnFmListFragmentListener(listener: OnFmListFragmentListener) {
        this.onFmListFragmentListener = listener
    }

    interface OnFmListFragmentListener {
        fun powerUpAsync(station: Int)
        fun isPlaying(): Boolean
    }



    override fun onDestroyView() {
        super.onDestroyView()
        // 清理资源
    }

}