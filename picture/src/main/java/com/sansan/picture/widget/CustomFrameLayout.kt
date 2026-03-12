package com.sansan.picture.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CustomFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val myScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var canTouch = true   //这个标志，用于处理触摸冲突

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        val actionIndex = ev.actionIndex // 触发当前ACTION的指针在事件中的索引

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

                myScope.launch {
                    delay(100L)
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