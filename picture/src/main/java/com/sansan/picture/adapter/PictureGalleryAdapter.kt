package com.sansan.picture.adapter

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.sansan.picture.R
import com.sansan.picture.model.Picture
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder

class PictureGalleryAdapter(
    private val mContext: Context
) : BaseBannerAdapter<Picture>() {

    override fun bindData(holder: BaseViewHolder<Picture>, bean: Picture?, position: Int, pageSize: Int) {
        val ivPicture: ImageView = holder.findViewById(R.id.banner_image)

        Glide.with(mContext).load(bean?.uri).into(ivPicture)

    }

    override fun getLayoutId(viewType: Int): Int {
        return R.layout.layout_picture_gallery_item;
    }
}