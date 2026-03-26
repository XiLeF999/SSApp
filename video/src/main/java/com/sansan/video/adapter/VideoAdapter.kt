package com.sansan.video.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.sansan.video.R
import com.sansan.video.model.Video
import com.sansan.video.utils.hide
import com.sansan.video.utils.show

class VideoAdapter(
    private val mContext: Context
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    private var videoList: List<Video> = emptyList()
    private var isManager: Boolean = false
    private var onItemClickListener: OnItemClickListener? = null
    private var onSelectItemListener: OnSelectItemListener? = null

    fun setData(list: List<Video>) {
        videoList = list

        notifyDataSetChanged()
    }

    fun setManager(manager: Boolean) {
        isManager = manager

        notifyDataSetChanged()

        onSelectItemListener?.onSelectNum(getSelectCount())
    }

    fun setAllSelect() {
        videoList.forEach {
            it.isSelect = true
        }

        notifyDataSetChanged()

        onSelectItemListener?.onSelectNum(getSelectCount())
    }

    fun setCancelManager() {
        videoList.forEach {
            it.isSelect = false
        }

        isManager = false

        notifyDataSetChanged()

        onSelectItemListener?.onSelectNum(getSelectCount())
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.layout_video_item, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = videoList[position]

        Glide.with(mContext)
            .asBitmap()
            .load(bean.path)
            .transform(
                MultiTransformation(
                    CenterCrop(),
                    RoundedCorners(8)
                )
            )
            .apply(
                RequestOptions()
                    .frame(0)  //第一帧
                    .override(256, 144)  // 缩略图大小
            )
            .into(holder.ivPicture)

        holder.tvDuration.text = bean.duration.let {
            val minutes = it / 1000 / 60
            val seconds = it / 1000 % 60
            String.format("%02d:%02d", minutes, seconds)
        }

        if(isManager){
            holder.ivSelect.show()

            holder.ivSelect.setOnClickListener {
                bean.isSelect = !bean.isSelect

                notifyItemChanged(position)

                onSelectItemListener?.onSelectNum(getSelectCount())

            }

            if (bean.isSelect){
                holder.ivSelect.setImageResource(R.mipmap.img_pic_set_ic_ok)
            } else {
                holder.ivSelect.setImageResource(R.mipmap.img_pic_set_ic_d)
            }


        } else {
            holder.ivSelect.hide()
        }


        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(videoList, bean, position)
        }

    }

    override fun getItemCount(): Int = videoList.size

    fun getList(): List<Video> = videoList


    fun getSelectCount(): Int {
        return videoList.count { it.isSelect }
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPicture: ImageView = itemView.findViewById(R.id.iv_picture)
        val ivSelect: ImageView = itemView.findViewById(R.id.iv_select)
        val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(videoList: List<Video>, bean: Video, position: Int)
    }

    fun setOnSelectItemListener(listener: OnSelectItemListener) {
        this.onSelectItemListener = listener
    }

    interface OnSelectItemListener {
        fun onSelectNum(num: Int)
    }


}