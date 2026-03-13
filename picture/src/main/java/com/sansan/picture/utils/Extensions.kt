package com.sansan.picture.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
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
 * 将时间戳（毫秒）转换为中文日期字符串
 */
fun Long.toChineseDate(): String {
    val sdf = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
    val date = Date(this)
    return sdf.format(date)
}

/**
 * 将毫秒时间戳转换为“下午13:51”格式
 */
fun Long.toChineseTime(): String {
    val instant = Instant.ofEpochMilli(this)
    val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())

    val hour = zonedDateTime.hour
    val minute = zonedDateTime.minute

    val amPm = if (hour < 12) "上午" else "下午"
    val hourStr = String.format("%02d", hour)
    val minuteStr = String.format("%02d", minute)

    return "$amPm${hourStr}:${minuteStr}"
}

/**
 * 图片Long型Size转两位小数KB
 */
fun Long.toKB2Decimal(): String = "%.2f".format(this / 1024.0) + " KB"



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


/**
 * 设置全屏
 */
fun Activity.setFullScreen() {
    window.decorView.apply {
        systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN
    }
}