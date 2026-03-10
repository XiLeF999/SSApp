package com.sansan.picture.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sansan.picture.adapter.PictureGalleryAdapter
import com.sansan.picture.databinding.ActivityPictureBinding
import com.sansan.picture.model.Picture
import com.sansan.picture.utils.setFullScreen
import com.sansan.picture.viewmodel.PictureViewModel
import kotlin.getValue

class PictureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPictureBinding

    private val viewModel: PictureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFullScreen()


        val pictureList = intent.getSerializableExtra("picture_list") as? ArrayList<Picture>
        val currentPicture = intent.getSerializableExtra("current_picture") as? Picture
        val position = intent.getIntExtra("position", 0)


        Log.e("wwww", "pictureList: $pictureList")
        Log.e("wwww", "currentPicture: $currentPicture")

        var pictureGalleryAdapter = PictureGalleryAdapter(this)

        binding.bannerViewPager.apply {
            setAdapter(pictureGalleryAdapter)
            setLifecycleRegistry(lifecycle)
            create()

            refreshData(pictureList)
        }



    }
}