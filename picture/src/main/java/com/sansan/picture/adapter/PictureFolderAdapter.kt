package com.sansan.picture.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.sansan.picture.R
import com.sansan.picture.model.PictureFolder

class PictureFolderAdapter(
    private val mContext: Context
) : RecyclerView.Adapter<PictureFolderAdapter.ViewHolder>() {

    private var pictureFolderList: List<PictureFolder> = emptyList()
    private var onItemClickListener: OnItemClickListener? = null

    fun setData(list: List<PictureFolder>) {
        pictureFolderList = list

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.layout_picture_folder_item, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = pictureFolderList[position]

        Glide.with(mContext).load(bean.coverPath).transform(
            MultiTransformation(
                CenterCrop(),
                RoundedCorners(8)
            )
        ).into(holder.ivCoverPicture)

        holder.itemView.setOnClickListener {
            it.setOnClickListener {
                onItemClickListener?.onItemClick(bean, position)
            }
        }

    }

    override fun getItemCount(): Int = pictureFolderList.size

    fun getList(): List<PictureFolder> = pictureFolderList


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCoverPicture: ImageView = itemView.findViewById(R.id.iv_cover_picture)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(bean: PictureFolder, position: Int)
    }


}