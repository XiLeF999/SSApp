package com.sansan.radio.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import com.sansan.radio.R

class QuitDialog(context: Context) : Dialog(context, R.style.QuitDialogStyle) {

    private var onQuitDialogClickListener: OnQuitDialogClickListener? = null

    private var tvSure: TextView? = null
    private var tvGoBack: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_quit_dialog)

        tvSure = findViewById(R.id.tv_sure)
        tvGoBack = findViewById(R.id.tv_go_back)

        tvSure?.setOnClickListener {

            onQuitDialogClickListener?.onQuit()

        }

        tvGoBack?.setOnClickListener {

            onQuitDialogClickListener?.onGoBack()

        }



    }



    fun setOnQuitDialogClickListener(listener: OnQuitDialogClickListener) {
        this.onQuitDialogClickListener = listener
    }

    interface OnQuitDialogClickListener {
        fun onQuit()
        fun onGoBack()
    }



}