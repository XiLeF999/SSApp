package com.sansan.picture.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sansan.picture.databinding.ItemPhotoBinding
import com.sansan.picture.model.Picture

class PhotoAdapter(
    private val context: Context
) : RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

    private var pictureList: List<Picture> = emptyList()

    var onTogglePhotoInfo: () -> Unit = {}
    var onShowGallery: () -> Unit = {}
    var onSwipeUp: () -> Unit = {}
    var onSwipeDown: () -> Unit = {}

    fun setData(list: List<Picture>) {
        pictureList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(pictureList[position])
    }

    override fun getItemCount(): Int = pictureList.size

    inner class ViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(picture: Picture) = binding.run {
            Glide.with(context).load(picture.uri).into(ivImage)
            ivImage.setOnSingleTapAction { onTogglePhotoInfo() }
            ivImage.setOnLongPressAction { onShowGallery() }
            ivImage.setOnSwipeUpAction { onSwipeUp() }
            ivImage.setOnSwipeDownAction { onSwipeDown() }
        }
    }
}