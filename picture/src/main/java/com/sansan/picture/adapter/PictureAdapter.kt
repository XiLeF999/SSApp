package com.sansan.picture.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sansan.picture.R
import com.sansan.picture.model.PictureModel

class PictureAdapter(
    private val mContext: Context
) : RecyclerView.Adapter<PictureAdapter.ViewHolder>() {

    private var pictureList: List<PictureModel> = emptyList()
    private var onItemClickListener: OnItemClickListener? = null

    fun setData(list: List<PictureModel>) {
        pictureList = list

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.layout_picture_item, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = pictureList[position]

        Glide.with(mContext).load(bean.uri).into(holder.ivPicture)

    }

    override fun getItemCount(): Int = pictureList.size

    fun getList(): List<PictureModel> = pictureList


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPicture: ImageView = itemView.findViewById(R.id.iv_picture)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(bean: PictureModel, position: Int)
    }


}