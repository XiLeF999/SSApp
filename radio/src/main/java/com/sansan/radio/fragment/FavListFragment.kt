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