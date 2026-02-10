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

class FavAdapter(
    private val mContext: Context
) : RecyclerView.Adapter<FavAdapter.ViewHolder>() {

    private var favList: List<StationInfo> = emptyList()
    private var onItemClickListener: OnItemClickListener? = null

    private var isPlayAnim: Boolean = true   //控制播放动画

    fun setData(list: List<StationInfo>, play: Boolean) {
        favList = list
        isPlayAnim = play

        notifyDataSetChanged()
    }

    fun setPlayAnim(play: Boolean) {
        isPlayAnim = play

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext)
            .inflate(R.layout.layout_radio_fav_item, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = favList[position]

        val animation = holder.ivAnim.drawable as AnimationDrawable
        if(bean.isSelect){
            holder.ivBg.setImageResource(R.mipmap.img_radio_select_bg)

            if(isPlayAnim){
                holder.ivAnim.post { animation.start() }
            } else {
                animation.stop()
            }

            holder.ivAnim.visibility = View.VISIBLE
            holder.tvId.visibility = View.INVISIBLE

        } else {
            holder.ivBg.setImageResource(R.color.color_transparent)

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

        holder.flItem.setOnClickListener {

            for (station in favList) {
                station.isSelect = false
            }
            bean.isSelect = true

            notifyDataSetChanged()

            onItemClickListener?.onItemClick(bean, position)
        }
    }

    override fun getItemCount(): Int = favList.size

    fun getList(): List<StationInfo> = favList


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flItem: FrameLayout = itemView.findViewById(R.id.fl_item)
        val ivBg: ImageView = itemView.findViewById(R.id.iv_bg)
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