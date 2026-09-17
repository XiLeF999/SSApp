package com.sansan.radio.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.sansan.radio.R
import com.sansan.radio.adapter.FmAdapter
import com.sansan.radio.bean.StationInfo
import com.sansan.radio.databinding.FragmentFmListBinding
import com.sansan.radio.fm.FmStation
import com.sansan.radio.utils.hide
import com.sansan.radio.utils.show

class FmListFragment : Fragment() {

    private lateinit var binding: FragmentFmListBinding

    private var mFmAdapter: FmAdapter? = null

    private var onFmListFragmentListener: OnFmListFragmentListener? = null

    private val isKy1045: Boolean
        get() = resources.getBoolean(R.bool.is_ky1045)

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

        if (stationInfoList.isEmpty()){
            binding.llNoStation.show()
            binding.rvFmList.hide()
            if (isKy1045) {
                setKy1045Illustration("radio_bg")
                binding.tvNoStation.text = "暂无可播放电台"
                binding.tvNoStation.setTextColor(requireContext().getColor(R.color.text_secondary))
            }
        } else {
            binding.llNoStation.hide()
            binding.rvFmList.show()

            stationInfoList.firstOrNull { it.station == currentStation }?.let { it.isSelect = true }
            mFmAdapter?.setData(stationInfoList, onFmListFragmentListener?.isPlaying() ?: true)
        }
    }

    fun setSearching(searching: Boolean) {
        if (!isKy1045 || !this::binding.isInitialized) return

        if (searching) {
            binding.rvFmList.hide()
            binding.llNoStation.show()
            setKy1045Illustration("radio_sle_bg")
            binding.tvNoStation.text = "正在搜索电台中..."
            binding.tvNoStation.setTextColor(requireContext().getColor(R.color.color_17A1E6))
        } else {
            updateStationList(FmStation.getCurrentStation(context))
        }
    }

    private fun setKy1045Illustration(resourceName: String) {
        val resourceId = resources.getIdentifier(resourceName, "drawable", requireContext().packageName)
        if (resourceId != 0) {
            binding.ivNoStation.setImageResource(resourceId)
        }
    }


    fun updatePlayAnim(isPlayAnim: Boolean){
        mFmAdapter?.setPlayAnim(isPlayAnim)
    }

    /**
     * 通过上一曲false、下一曲true
     * 更新当前播放的电台
     */
    fun updateStationItemInternal(isNext: Boolean) {
        val stationInfoList = mFmAdapter?.getList() ?: return
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

        onFmListFragmentListener?.powerUpAsync(stationInfoList[newPosition].station)

        // 更新适配器
        mFmAdapter?.setData(stationInfoList, onFmListFragmentListener?.isPlaying() ?: true)
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
