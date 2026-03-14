package com.sansan.picture.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import com.sansan.picture.R

class DeletePictureDialog(context: Context) : Dialog(context, R.style.DeletePictureDialogStyle) {

    private var onPictureDialogClickListener: OnPictureDialogClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_delete_picture_dialog)

        findViewById<View>(R.id.tv_cancel).setOnClickListener { dismiss() }

        findViewById<View>(R.id.tv_sure).setOnClickListener {

            onPictureDialogClickListener?.onSure()

        }

    }




    fun setOnPictureDialogClickListener(listener: OnPictureDialogClickListener) {
        this.onPictureDialogClickListener = listener
    }

    interface OnPictureDialogClickListener {
        fun onSure()
    }




}