package com.sansan.picture.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class ScrollableRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val scrollBarDecoration = DraggableScrollBarDecoration(context)

    init {
        // 添加滚动条装饰器
        addItemDecoration(scrollBarDecoration)

        // 监听 RecyclerView 的滚动，自动显示滚动条
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == SCROLL_STATE_DRAGGING) {
                    scrollBarDecoration.showScrollBarTemporarily()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0) {
                    scrollBarDecoration.showScrollBarTemporarily()
                }
            }
        })
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // 优先处理滚动条触摸事件
        if (scrollBarDecoration.handleTouchEvent(event, this)) {
            return true
        }
        return super.dispatchTouchEvent(event)
    }
}