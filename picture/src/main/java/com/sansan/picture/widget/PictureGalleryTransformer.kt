package com.sansan.picture.widget

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class PictureGalleryTransformer : ViewPager2.PageTransformer {
    private val minScale = 0.85f  // 两边图片的缩放比例
    private val minAlpha = 0.5f   // 两边图片的透明度

    override fun transformPage(page: View, position: Float) {
        val pageWidth = page.width
        val absPosition = abs(position)

        // 1. 缩放
        val scale = if (absPosition <= 1) {
            minScale + (1 - minScale) * (1 - absPosition)
        } else {
            minScale
        }

        page.scaleX = scale
        page.scaleY = scale

        // 2. 透明度
        page.alpha = minAlpha + (1 - minAlpha) * (1 - absPosition)

        // 3. 水平偏移（让两边图片分开）
        val offset = pageWidth * (1 - scale) / 2
        page.translationX = if (position > 0) -offset else offset
    }
}