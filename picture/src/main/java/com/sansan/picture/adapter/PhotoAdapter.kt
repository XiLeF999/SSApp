package com.sansan.picture.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sansan.picture.databinding.ItemPhotoBinding
import com.sansan.picture.model.Picture

class PhotoAdapter(private val mContext: Context) :
    RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

    private var pictureList: List<Picture> = emptyList()

    lateinit var onHidePhotoListener: (Float) -> Unit
    lateinit var onTogglePhotoInfo: () -> Unit

    fun setData(list: List<Picture>) {
        pictureList = list

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding, onHidePhotoListener, onTogglePhotoInfo)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(pictureList[position])
    }

    override fun getItemCount(): Int = pictureList.size

    inner class ViewHolder(
        private val binding: ItemPhotoBinding,
        private val onNeedHidePhoto: (Float) -> Unit,
        private val onTogglePhotoInfo: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(picture: Picture) = binding.run {

            Glide.with(mContext).load(picture.uri).into(ivImage)

            ivImage.setOnScaleChangeListener { _, _, _ ->
                onNeedHidePhoto(ivImage.scale)
            }
            ivImage.setOnClickListener {
                onTogglePhotoInfo()
            }
        }
    }


}