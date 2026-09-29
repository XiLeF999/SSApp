package com.sansan.video.widget

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VideoItemDecoration(private val spanCount: Int, private val spacingX: Int, private val spacingY: Int, private val isKy1045: Boolean) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        if (!isKy1045) {
            val column = position % spanCount
            outRect.left = spacingX * (spanCount - column) / spanCount
            outRect.right = spacingX * (column + 1) / spanCount
            if (position >= spanCount) outRect.top = spacingY
            outRect.bottom = spacingY
            return
        }
        val params = view.layoutParams as? GridLayoutManager.LayoutParams ?: return
        if (params.spanSize == spanCount) {
            // Date headers sit directly against the first video row.
            outRect.bottom = 0
            return
        }
        val column = params.spanIndex
        outRect.left = column * spacingX / spanCount
        outRect.right = spacingX - (column + 1) * spacingX / spanCount
        outRect.bottom = spacingY
    }
}
