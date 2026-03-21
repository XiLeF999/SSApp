package com.sansan.video.widget

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatImageView

class RotateImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    private var isRotating = false

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animator ->
            val value = animator.animatedValue as Float
            rotation = value
        }
    }

    fun startRotate(duration: Long = 1000) {
        if (!isRotating) {
            animator.duration = duration
            animator.start()
            isRotating = true
        }
    }

    fun stopRotate() {
        if (isRotating) {
            animator.cancel()
            isRotating = false
            rotation = 0f
        }
    }

    override fun callOnClick(): Boolean {
        return if (isRotating) true
        else super.callOnClick()
    }
}