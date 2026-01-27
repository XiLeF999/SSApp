package com.sansan.radio.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.EditText
import android.widget.Toast

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