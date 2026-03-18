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

    private var llDelete: LinearLayout? = null
    private var flDeleteProgress: FrameLayout? = null
    private var flDeleteSuccess: FrameLayout? = null
    private var tvSure: TextView? = null
    private var tvCancel: TextView? = null
    private var ivProgress: RotateImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_delete_picture_dialog)

        llDelete = findViewById(R.id.ll_delete)
        flDeleteProgress = findViewById(R.id.fl_delete_progress)
        flDeleteSuccess = findViewById(R.id.fl_delete_success)
        tvSure = findViewById(R.id.tv_sure)
        tvCancel = findViewById(R.id.tv_cancel)
        ivProgress = findViewById(R.id.iv_progress)

        tvCancel?.setOnClickListener { dismiss() }

        tvSure?.setOnClickListener {

            llDelete?.hide()
            flDeleteSuccess?.hide()
            flDeleteProgress?.show()
            ivProgress?.startRotate()

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