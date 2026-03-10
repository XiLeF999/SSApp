package com.sansan.picture.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.sansan.picture.adapter.PictureGalleryAdapter
import com.sansan.picture.databinding.ActivityPictureBinding
import com.sansan.picture.model.Picture
import com.sansan.picture.repository.PictureRepository.StorageType
import com.sansan.picture.utils.hide
import com.sansan.picture.utils.setFullScreen
import com.sansan.picture.utils.show
import com.sansan.picture.viewmodel.PictureViewModel
import com.sansan.picture.widget.PictureGalleryTransformer
import kotlin.getValue

class PictureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPictureBinding

    private val viewModel: PictureViewModel by viewModels()

    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private lateinit var pictureList: ArrayList<Picture>
    private lateinit var currentPicture: Picture
    private var position: Int = 0

    private var currentScale = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFullScreen()


        pictureList = intent.getSerializableExtra("picture_list") as ArrayList<Picture>
        currentPicture = intent.getSerializableExtra("current_picture") as Picture
        position = intent.getIntExtra("position", 0)

        Glide.with(this).load(currentPicture.uri).into(binding.ivPicture)

        // 简化版双指缩小监听
        scaleGestureDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                // 只修改 onScale 方法
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor

                    // 修复：实时更新图片缩放
                    if (scaleFactor < 1.0) {  // 只在缩小时更新
                        // 更新当前缩放比例
                        currentScale *= scaleFactor

                        // 限制最小缩放
                        if (currentScale < 0.3f) currentScale = 0.3f

                        // 应用到图片
                        binding.ivPicture.scaleX = currentScale
                        binding.ivPicture.scaleY = currentScale
                        binding.ivPicture.alpha = currentScale

                        return true
                    }
                    return false
                }

                // 在 onScaleEnd 中检查
                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    // 检查是否需要触发过渡动画
                    if (currentScale < 0.5f) {  // 缩小到50%以下
                        startSimpleTransition()
                    } else {
                        // 恢复原状
                        binding.ivPicture.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                        currentScale = 1.0f
                    }
                }
            })

        binding.ivPicture.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun startSimpleTransition() {
        // 1. 图片缩小消失
        binding.ivPicture.animate()
            .scaleX(0.5f)
            .scaleY(0.5f)
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.ivPicture.hide()

                    // 2. 显示Carousel
                    binding.carouselRecyclerview.apply {
                        adapter = PictureGalleryAdapter(this@PictureActivity, pictureList)
                        set3DItem(false)
                        setAlpha(true)
                        setInfinite(true)
                        setIntervalRatio(0.352f)

                        // 初始状态：放大
                        scaleX = 1.5f
                        scaleY = 1.5f
                        alpha = 0f
                        show()

                        // 缩小+淡入
                        animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                    }
                }
            })
            .start()
    }
}