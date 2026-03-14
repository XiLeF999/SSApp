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
import com.sansan.picture.model.Picture
import com.sansan.picture.utils.hide
import com.sansan.picture.utils.show

class PictureAdapter(
    private val mContext: Context
) : RecyclerView.Adapter<PictureAdapter.ViewHolder>() {

    private var pictureList: List<Picture> = emptyList()
    private var isManager: Boolean = false
    private var onItemClickListener: OnItemClickListener? = null

    fun setData(list: List<Picture>) {
        pictureList = list

        notifyDataSetChanged()
    }

    fun setManager(manager: Boolean) {
        isManager = manager

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.layout_picture_item, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = pictureList[position]

        Glide.with(mContext).load(bean.uri).transform(
            MultiTransformation(
                CenterCrop(),
                RoundedCorners(8)
            )
        ).into(holder.ivPicture)

        if(isManager){
            holder.ivSelect.show()

            holder.ivSelect.setOnClickListener {
                bean.isSelect = !bean.isSelect

                notifyItemChanged(position)
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
            onItemClickListener?.onItemClick(pictureList, bean, position)
        }

    }

    override fun getItemCount(): Int = pictureList.size

    fun getList(): List<Picture> = pictureList


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPicture: ImageView = itemView.findViewById(R.id.iv_picture)
        val ivSelect: ImageView = itemView.findViewById(R.id.iv_select)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(pictureList: List<Picture>, bean: Picture, position: Int)
    }


}