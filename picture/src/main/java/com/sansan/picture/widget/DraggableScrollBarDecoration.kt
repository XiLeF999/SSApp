package com.sansan.picture.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min

class DraggableScrollBarDecoration(context: Context) : RecyclerView.ItemDecoration() {

    // 滚动条尺寸
    private var scrollBarWidth = 0f
    private var scrollBarPadding = 0f
    private var scrollBarRadius = 0f

    // 滚动条位置
    private var scrollBarRect = RectF()
    private var scrollBarThumbRect = RectF()

    // 绘画
    private val scrollBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scrollBarThumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scrollBarThumbDraggingPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 状态
    private var isDragging = false
    private var isShowScrollBar = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // 自动隐藏相关
    private var hideRunnable = Runnable { hideScrollBar() }
    private val HIDE_DELAY = 2000L

    init {
        // 初始化尺寸 (8dp宽度，4dp内边距，4dp圆角)
        scrollBarWidth = dpToPx(context, 8f)
        scrollBarPadding = dpToPx(context, 4f)
        scrollBarRadius = dpToPx(context, 4f)

        // 滚动条轨道样式
        scrollBarPaint.color = Color.parseColor("#00000000")

        // 滚动条滑块默认样式
        scrollBarThumbPaint.color = Color.parseColor("#66000000")

        // 滚动条滑块拖动时样式
        scrollBarThumbDraggingPaint.color = Color.parseColor("#99000000")
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        // 不需要滚动时隐藏滚动条
        if (!parent.canScrollVertically(1) && !parent.canScrollVertically(-1)) {
            isShowScrollBar = false
            return
        }

        // 计算滚动条位置
        computeScrollBarPosition(parent)

        // 绘制滚动条轨道
        c.drawRoundRect(scrollBarRect, scrollBarRadius, scrollBarRadius, scrollBarPaint)

        // 绘制滚动条滑块
        val thumbPaint = if (isDragging) scrollBarThumbDraggingPaint else scrollBarThumbPaint
        c.drawRoundRect(scrollBarThumbRect, scrollBarRadius, scrollBarRadius, thumbPaint)

        // 延迟隐藏滚动条
        if (isShowScrollBar && !isDragging) {
            parent.removeCallbacks(hideRunnable)
            parent.postDelayed(hideRunnable, HIDE_DELAY)
        }
    }

    /**
     * 计算滚动条位置
     */
    private fun computeScrollBarPosition(parent: RecyclerView) {
        val viewHeight = parent.height
        val scrollRange = parent.computeVerticalScrollRange()
        val scrollOffset = parent.computeVerticalScrollOffset()
        val scrollExtent = parent.computeVerticalScrollExtent()

        // 滚动条可见高度比例
        val thumbHeightRatio = scrollExtent.toFloat() / scrollRange
        val thumbHeight = max(viewHeight * thumbHeightRatio, dpToPx(parent.context, 40f))

        // 滚动条位置
        val left = parent.width - scrollBarWidth - scrollBarPadding
        val right = left + scrollBarWidth

        // 滚动条轨道
        scrollBarRect.set(
            left,
            scrollBarPadding,
            right,
            viewHeight - scrollBarPadding
        )

        // 计算滑块位置
        val availableHeight = viewHeight - 2 * scrollBarPadding - thumbHeight
        val thumbPositionRatio = if (scrollRange - scrollExtent > 0) {
            scrollOffset.toFloat() / (scrollRange - scrollExtent)
        } else {
            0f
        }

        val thumbTop = scrollBarPadding + availableHeight * thumbPositionRatio
        scrollBarThumbRect.set(
            left,
            thumbTop,
            right,
            thumbTop + thumbHeight
        )

        isShowScrollBar = true
    }

    /**
     * 处理触摸事件
     */
    fun handleTouchEvent(event: MotionEvent, parent: RecyclerView): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y

                // 检查是否点击了滚动条滑块
                if (isTouchInScrollBarThumb(event.x, event.y)) {
                    isDragging = true
                    isShowScrollBar = true
                    parent.removeCallbacks(hideRunnable)
                    parent.invalidateItemDecorations()
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val deltaY = event.y - lastTouchY

                    // 根据拖动距离计算滚动位置
                    if (deltaY != 0f) {
                        scrollByDrag(deltaY, parent)
                    }

                    lastTouchY = event.y
                    parent.invalidateItemDecorations()
                    return true
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    parent.invalidateItemDecorations()

                    // 拖动结束后延迟隐藏
                    parent.removeCallbacks(hideRunnable)
                    parent.postDelayed(hideRunnable, HIDE_DELAY)
                    return true
                }
            }
        }

        return false
    }

    /**
     * 点击滚动条轨道快速定位
     */
    private fun handleQuickScroll(event: MotionEvent, parent: RecyclerView): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            // 检查是否点击了滚动条轨道
            if (isTouchInScrollBarTrack(event.x, event.y) &&
                !isTouchInScrollBarThumb(event.x, event.y)) {

                val viewHeight = parent.height
                val scrollRange = parent.computeVerticalScrollRange()
                val scrollExtent = parent.computeVerticalScrollExtent()

                // 计算点击位置比例
                val clickPosition = (event.y - scrollBarPadding) /
                        (viewHeight - 2 * scrollBarPadding)

                // 计算目标滚动位置
                val targetScroll = (clickPosition * (scrollRange - scrollExtent)).toInt()
                parent.scrollBy(0, targetScroll - parent.computeVerticalScrollOffset())

                // 显示滚动条
                isShowScrollBar = true
                parent.removeCallbacks(hideRunnable)
                parent.postDelayed(hideRunnable, HIDE_DELAY)

                return true
            }
        }
        return false
    }

    /**
     * 通过拖动滚动
     */
    private fun scrollByDrag(deltaY: Float, parent: RecyclerView) {
        val viewHeight = parent.height
        val scrollRange = parent.computeVerticalScrollRange()
        val scrollExtent = parent.computeVerticalScrollExtent()
        val availableHeight = viewHeight - 2 * scrollBarPadding

        if (availableHeight > 0) {
            val dragRatio = deltaY / availableHeight
            val scrollDistance = (dragRatio * (scrollRange - scrollExtent)).toInt()

            parent.scrollBy(0, scrollDistance)
        }
    }

    /**
     * 判断是否触摸在滚动条滑块上
     */
    private fun isTouchInScrollBarThumb(touchX: Float, touchY: Float): Boolean {
        val touchRect = RectF(
            scrollBarThumbRect.left - scrollBarPadding * 2,
            scrollBarThumbRect.top - scrollBarPadding * 2,
            scrollBarThumbRect.right + scrollBarPadding * 2,
            scrollBarThumbRect.bottom + scrollBarPadding * 2
        )
        return touchRect.contains(touchX, touchY)
    }

    /**
     * 判断是否触摸在滚动条轨道上
     */
    private fun isTouchInScrollBarTrack(touchX: Float, touchY: Float): Boolean {
        val touchRect = RectF(
            scrollBarRect.left - scrollBarPadding * 2,
            scrollBarRect.top,
            scrollBarRect.right + scrollBarPadding * 2,
            scrollBarRect.bottom
        )
        return touchRect.contains(touchX, touchY)
    }

    /**
     * 隐藏滚动条
     */
    fun hideScrollBar() {
        isShowScrollBar = false
    }

    /**
     * 显示滚动条
     */
    fun showScrollBarTemporarily() {
        isShowScrollBar = true
    }

    private fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }
}