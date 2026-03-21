package com.sansan.video.widget

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class VideoItemDecoration(
    private val spanCount: Int,  // 列数
    private val spacingX: Int,    // 水平间距
    private val spacingY: Int // 垂直间距
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // 获取当前item的位置

        if (position >= 0) {
            val column = position % spanCount // 当前item在第几列(0-based)

            // 水平间距计算
            outRect.left = (spacingX * (spanCount - column) / spanCount)
            outRect.right = (spacingX * (column + 1) / spanCount)

            // 垂直间距
            if (position >= spanCount) { // 如果不是第一行
                outRect.top = spacingY
            }

            // 也可以为最后一行不设置底部间距（可选）
            // val totalItemCount = state.itemCount
            // if (position >= totalItemCount - spanCount) {
            //     outRect.bottom = spacingY
            // }

            outRect.bottom = spacingY // 简单起见，所有item都有底部间距
        }
    }
}