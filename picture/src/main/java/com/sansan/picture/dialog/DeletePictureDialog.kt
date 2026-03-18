package com.sansan.picture.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.sansan.picture.R
import com.sansan.picture.utils.hide
import com.sansan.picture.utils.show
import com.sansan.picture.widget.RotateImageView

class DeletePictureDialog(context: Context) : Dialog(context, R.style.DeletePictureDialogStyle) {

    private var onPictureDialogClickListener: OnPictureDialogClickListener? = null

    private val llDelete by lazy { findViewById<LinearLayout>(R.id.ll_delete) }
    private val flDeleteProgress by lazy { findViewById<FrameLayout>(R.id.fl_delete_progress) }
    private val flDeleteSuccess by lazy { findViewById<FrameLayout>(R.id.fl_delete_success) }
    private val tvSure by lazy { findViewById<TextView>(R.id.tv_sure) }
    private val tvCancel by lazy { findViewById<TextView>(R.id.tv_cancel) }
    private val ivProgress by lazy { findViewById<RotateImageView>(R.id.iv_progress) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_delete_picture_dialog)

        tvCancel.setOnClickListener { dismiss() }

        tvSure.setOnClickListener {

            llDelete?.hide()
            flDeleteSuccess?.hide()
            flDeleteProgress?.show()
            ivProgress.startRotate()

            onPictureDialogClickListener?.onSure()

        }

    }

    fun showDeleteDialog() {
        llDelete?.show()
        flDeleteSuccess?.hide()
        flDeleteProgress?.hide()
        show()
    }


    fun deleteSuccess() {
        llDelete?.hide()
        flDeleteSuccess?.show()
        flDeleteProgress?.hide()
        ivProgress?.stopRotate()
    }

    fun deleteFail() {
        llDelete?.show()
        flDeleteSuccess?.hide()
        flDeleteProgress?.hide()
        ivProgress?.stopRotate()
        dismiss()
    }




    fun setOnPictureDialogClickListener(listener: OnPictureDialogClickListener) {
        this.onPictureDialogClickListener = listener
    }

    interface OnPictureDialogClickListener {
        fun onSure()
    }




}