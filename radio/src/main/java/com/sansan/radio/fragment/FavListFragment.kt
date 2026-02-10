package com.sansan.radio.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.sansan.radio.adapter.FavAdapter
import com.sansan.radio.bean.StationInfo
import com.sansan.radio.databinding.FragmentFavListBinding
import com.sansan.radio.fm.FmStation

class FavListFragment : Fragment() {

    private lateinit var binding: FragmentFavListBinding

    private var mFavAdapter: FavAdapter? = null

    private var onFavListFragmentListener: OnFavListFragmentListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFavListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 初始化视图和逻辑
        mFavAdapter = context?.let { FavAdapter(it) }
        binding.rvFavList.layoutManager = LinearLayoutManager(context)
        binding.rvFavList.adapter = mFavAdapter

        updateStationList(FmStation.getCurrentStation(context))


        mFavAdapter?.setOnItemClickListener(object : FavAdapter.OnItemClickListener {
            override fun onItemClick(bean: StationInfo, position: Int) {

                onFavListFragmentListener?.powerUpAsync(bean.station)


            }
        })

    }



    fun updateStationList(currentStation: Int){

        val stationInfoList = FmStation.getFavStation(context)

        stationInfoList.firstOrNull { it.station == currentStation }?.let { it.isSelect = true }

        mFavAdapter?.setData(stationInfoList, onFavListFragmentListener?.isPlaying() ?: true)
    }



    fun updatePlayAnim(isPlayAnim: Boolean){
        mFavAdapter?.setPlayAnim(isPlayAnim)
    }


    /**
     * 通过上一曲false、下一曲true
     * 更新当前播放的电台
     */
    fun updateStationItemInternal(isNext: Boolean) {
        val stationInfoList = mFavAdapter?.getList() ?: return
        if (stationInfoList.isEmpty()) return

        val currentStation = FmStation.getCurrentStation(context)

        // 查找当前选中的位置
        var newPosition = stationInfoList.indexOfFirst { it.isSelect }

        if (newPosition == -1) { // 没找到选中的
            if (isNext) {
                // 下一个：找第一个大于当前频率的
                newPosition = stationInfoList.indexOfFirst { it.station > currentStation }
            } else {
                // 上一个：找最后一个小于当前频率的
                newPosition = stationInfoList.indexOfLast { it.station < currentStation }
            }

            if (newPosition == -1) { // 没找到
                newPosition = if (isNext) 0 else stationInfoList.size - 1
            }
        } else { // 找到了选中的
            newPosition = if (isNext) {
                // 下一个：循环前进
                (newPosition + 1) % stationInfoList.size
            } else {
                // 上一个：循环后退
                (newPosition - 1 + stationInfoList.size) % stationInfoList.size
            }
        }

        // 更新选中状态
        stationInfoList.forEach { it.isSelect = false }
        stationInfoList[newPosition].isSelect = true

        // 更新适配器
        mFavAdapter?.setData(stationInfoList, onFavListFragmentListener?.isPlaying() ?: true)
    }



    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateStationList(FmStation.getCurrentStation(context))
        }
    }


    fun setOnFavListFragmentListener(listener: OnFavListFragmentListener) {
        this.onFavListFragmentListener = listener
    }

    interface OnFavListFragmentListener {
        fun powerUpAsync(station: Int)
        fun isPlaying(): Boolean
    }



    override fun onDestroyView() {
        super.onDestroyView()
        // 清理资源
    }

}