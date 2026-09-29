package com.sansan.picture.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import com.sansan.picture.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PictureFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val pictureScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val isKy1045 = resources.getBoolean(R.bool.is_ky1045)

    var canTouch = true   //这个标志，用于处理触摸冲突

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // PhotoView needs both tap sequences to recognize a double tap. The legacy
        // inter-gesture lock can swallow the second DOWN on very fast 1045 taps.
        if (isKy1045) {
            return if (canTouch) super.dispatchTouchEvent(ev) else true
        }

        val action = ev.actionMasked

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (canTouch){
                    canTouch = false

                } else {
                    return  true
                }

            }
            MotionEvent.ACTION_POINTER_DOWN -> {
            }
            MotionEvent.ACTION_POINTER_UP -> {
            }
            MotionEvent.ACTION_UP -> {

                pictureScope.launch {
                    delay(30L)    //这个延迟是为了在双指抬起一个手指时，会马上执行一次ACTION_DOWN，导致误执行一次点击事件，给个延迟不误执行
                    canTouch = true
                }

            }
            MotionEvent.ACTION_CANCEL -> {

            }

        }

        return super.dispatchTouchEvent(ev)
    }



    fun setPictureCanTouch(canTouch: Boolean) {
        this.canTouch = canTouch
    }


}
