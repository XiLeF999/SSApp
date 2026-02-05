package com.sansan.radio.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sansan.radio.R
import com.sansan.radio.bean.StationInfo
import com.sansan.radio.fm.FmUtils

class FmAdapter(
    private val mContext: Context
) : RecyclerView.Adapter<FmAdapter.ViewHolder>() {

    private var fmList: List<StationInfo> = emptyList()
    private var onItemClickListener: OnItemClickListener? = null

    fun setData(list: List<StationInfo>) {
        fmList = list

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext)
            .inflate(R.layout.layout_radio_fm_item, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = fmList[position]

        holder.tvFrequency.text = FmUtils.computeFrequency(bean.station).toString() + "MHz"

        holder.llItem.setOnClickListener {
            onItemClickListener?.onItemClick(bean, position)
        }
    }

    override fun getItemCount(): Int = fmList.size


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llItem: FrameLayout = itemView.findViewById(R.id.ll_item)
        val tvFrequency: TextView = itemView.findViewById(R.id.tv_frequency)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(bean: StationInfo, position: Int)
    }


}