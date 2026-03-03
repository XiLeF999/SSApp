package com.sansan.picture.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import kotlin.math.roundToInt

/**  扩展函数  */

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun EditText.textString() = text.toString()

fun String?.isNotNullOrEmpty() = !this.isNullOrEmpty()

inline fun <reified T : Activity> Context.startActivity() {
    startActivity(Intent(this, T::class.java))
}

/**
 * 保留一位小数
 */
fun Float.roundToOneDecimal(): Float = (this * 10).roundToInt() / 10f

/**
 * 一位数在前面加0
 */
fun Int.toTwoDigits(): String = if (this < 10) "0$this" else toString()


/**
 * 设置状态栏沉浸式透明
 */
fun Activity.setTransparentStatusBar() {
    window.apply {
        // 清除半透明标志
        clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        // 允许绘制系统栏背景
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        // 设置状态栏颜色为透明
        statusBarColor = Color.TRANSPARENT

        decorView.apply {
            systemUiVisibility = systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }
}