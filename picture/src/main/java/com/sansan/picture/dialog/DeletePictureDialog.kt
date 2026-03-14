package com.sansan.picture.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import com.sansan.picture.R

class DeletePictureDialog(context: Context) : Dialog(context, R.style.DeletePictureDialogStyle), View.OnClickListener {

    private lateinit var ivClose: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_delete_picture_dialog)

//        ivClose = findViewById(R.id.iv_close)
//        ivClose.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_close -> dismiss()
        }
    }
}