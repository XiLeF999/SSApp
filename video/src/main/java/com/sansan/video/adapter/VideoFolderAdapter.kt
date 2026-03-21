package com.sansan.video.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sansan.video.R
import com.sansan.video.model.VideoFolder

class VideoFolderAdapter(
    private val mContext: Context
) : RecyclerView.Adapter<VideoFolderAdapter.ViewHolder>() {

    private var videoFolderList: List<VideoFolder> = emptyList()
    private var onItemClickListener: OnItemClickListener? = null

    fun setData(list: List<VideoFolder>) {
        videoFolderList = list

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.layout_video_folder_item, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = videoFolderList[position]

//        Glide.with(mContext).load(bean.coverPath).transform(
//            MultiTransformation(
//                CenterCrop(),
//                RoundedCorners(8)
//            )
//        ).into(holder.ivCoverVideo)

        holder.tvName.text = bean.name
        holder.tvCount.text = bean.count.toString()

        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(bean, position)
        }

    }

    override fun getItemCount(): Int = videoFolderList.size

    fun getList(): List<VideoFolder> = videoFolderList


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val ivCoverVideo: ImageView = itemView.findViewById(R.id.iv_cover_video)
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvCount: TextView = itemView.findViewById(R.id.tv_count)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(bean: VideoFolder, position: Int)
    }


}