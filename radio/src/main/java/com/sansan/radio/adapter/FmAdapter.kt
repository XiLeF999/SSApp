package com.sansan.radio.adapter

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.sansan.radio.R
import com.sansan.radio.bean.StationInfo
import com.sansan.radio.fm.FmUtils
import com.sansan.radio.utils.toTwoDigits

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

        holder.llItem.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = if (position == 0) 0 else (-14)
        }

        holder.llItem.isSelected = bean.isSelect

        val animation = holder.ivAnim.drawable as AnimationDrawable
        if(bean.isSelect){
            holder.llItem.background = mContext.getDrawable(R.mipmap.img_radio_select_bg)

            holder.ivAnim.post { animation.start() }

            holder.ivAnim.visibility = View.VISIBLE
            holder.tvId.visibility = View.INVISIBLE

        } else {
            holder.llItem.background = mContext.getDrawable(R.color.color_transparent)

            animation.stop()

            holder.ivAnim.visibility = View.INVISIBLE
            holder.tvId.visibility = View.VISIBLE
        }

        if(bean.isFavorite == 0){   //false
            holder.ivFav.setImageResource(R.mipmap.img_radio_list_btn_fav_select)
        } else {
            holder.ivFav.setImageResource(R.mipmap.img_radio_list_btn_fav_select_ed)
        }

        holder.tvId.text = (position + 1).toTwoDigits()
        holder.tvFrequency.text = FmUtils.computeFrequency(bean.station).toString() + "MHz"

        holder.llItem.setOnClickListener {

            for (station in fmList) {
                station.isSelect = false
            }
            bean.isSelect = true

            notifyDataSetChanged()

            onItemClickListener?.onItemClick(bean, position)
        }
    }

    override fun getItemCount(): Int = fmList.size


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llItem: FrameLayout = itemView.findViewById(R.id.ll_item)
        val tvId: TextView = itemView.findViewById(R.id.tv_id)
        val ivAnim: ImageView = itemView.findViewById(R.id.iv_anim)
        val tvFrequency: TextView = itemView.findViewById(R.id.tv_frequency)
        val ivFav: ImageView = itemView.findViewById(R.id.iv_fav)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(bean: StationInfo, position: Int)
    }


}