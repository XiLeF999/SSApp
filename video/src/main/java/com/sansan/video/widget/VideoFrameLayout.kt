package com.sansan.video.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout

class VideoFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var onVideoFrameLayoutClickListener: OnVideoFrameLayoutClickListener? = null

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                onVideoFrameLayoutClickListener?.onVideoFrameLayoutClick()
            }
        }
        return super.dispatchTouchEvent(ev)
    }



    fun setOnVideoFrameLayoutClickListener(listener: OnVideoFrameLayoutClickListener) {
        this.onVideoFrameLayoutClickListener = listener
    }

    interface OnVideoFrameLayoutClickListener {
        fun onVideoFrameLayoutClick()
    }

}