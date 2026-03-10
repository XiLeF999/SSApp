package com.sansan.picture.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.sansan.picture.adapter.PictureGalleryAdapter
import com.sansan.picture.databinding.ActivityPictureBinding
import com.sansan.picture.model.Picture
import com.sansan.picture.utils.setFullScreen
import com.sansan.picture.viewmodel.PictureViewModel
import com.sansan.picture.widget.PictureGalleryTransformer
import kotlin.getValue

class PictureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPictureBinding

    private val viewModel: PictureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFullScreen()


        val pictureList = intent.getSerializableExtra("picture_list") as ArrayList<Picture>
        val currentPicture = intent.getSerializableExtra("current_picture") as Picture
        val position = intent.getIntExtra("position", 0)

        Glide.with(this).load(currentPicture.uri).into(binding.ivPicture)



        binding.carouselRecyclerview.apply {
            adapter = PictureGalleryAdapter(this@PictureActivity, pictureList)
            set3DItem(false)
            setAlpha(true)
            setInfinite(true)
            setIntervalRatio(0.352f)  //这个值是根据布局宽度调试出来的，正好不重叠且挨着
        }



    }
}