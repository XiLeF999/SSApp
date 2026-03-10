package com.sansan.picture.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sansan.picture.R
import com.sansan.picture.model.Picture

class PictureGalleryAdapter(private val mContext: Context) :
    RecyclerView.Adapter<PictureGalleryAdapter.ViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null

    private var pictureList: List<Picture> = emptyList()

    fun setData(list: List<Picture>) {
        pictureList = list

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_picture_gallery_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(mContext).load(pictureList[position].uri).into(holder.imageView)

        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(pictureList, pictureList[position], position)
        }

    }

    override fun getItemCount() = pictureList.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.iv_image)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(pictureList: List<Picture>, bean: Picture, position: Int)
    }

}