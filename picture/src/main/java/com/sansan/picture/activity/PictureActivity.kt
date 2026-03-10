package com.sansan.picture.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
        val currentPicture = intent.getSerializableExtra("current_picture") as? Picture
        val position = intent.getIntExtra("position", 0)


        Log.e("wwww", "pictureList: $pictureList")
        Log.e("wwww", "currentPicture: $currentPicture")

        // 1. 设置适配器
        binding.viewPager2.adapter = PictureGalleryAdapter(this, pictureList)

        // 2. 设置页面间距
        val spacingPx = 20
        binding.viewPager2.setPadding(spacingPx, 0, spacingPx, 0)
        binding.viewPager2.clipToPadding = false
        binding.viewPager2.clipChildren = false

        // 3. 设置页面变换（核心）
        binding.viewPager2.setPageTransformer(PictureGalleryTransformer())

        // 4. 预加载
        binding.viewPager2.offscreenPageLimit = 2



    }
}