package com.sansan.picture.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
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

    private lateinit var pictureGalleryAdapter: PictureGalleryAdapter

    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private lateinit var pictureList: ArrayList<Picture>
//    private lateinit var currentPicture: Picture
    private var currentPosition: Int = 0

    private var currentScale = 1.0f
    private var isTransitioning = false
    private var isAutoTransitioning = false  // 标记是否正在自动过渡

    private var transitionTriggered = false

    private var currentScrollerState : Int = RecyclerView.SCROLL_STATE_IDLE   //是否在滚动

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFullScreen()


        val pList = intent.getSerializableExtra("picture_list") as ArrayList<Picture>
//        currentPicture = intent.getSerializableExtra("current_picture") as Picture
        val pos = intent.getIntExtra("position", 0)

        currentPosition = pos

        //重新排序列表:让选择的图片始终在第一个
        pictureList = reorderList(pList, currentPosition)
        currentPosition = 0

        Glide.with(this).load(pictureList[currentPosition].uri).into(binding.ivPicture)


        pictureGalleryAdapter = PictureGalleryAdapter(this)
        pictureGalleryAdapter.setData(pictureList)
        binding.carouselRecyclerview.adapter = pictureGalleryAdapter

        scaleGestureDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    if (isTransitioning || isAutoTransitioning) return false

                    val scaleFactor = detector.scaleFactor

                    if (scaleFactor < 1.0) {  // 只在缩小时处理
                        currentScale *= scaleFactor

                        // 限制最小缩放
                        if (currentScale < 0.3f) currentScale = 0.3f

                        // 应用缩放
                        binding.ivPicture.scaleX = currentScale
                        binding.ivPicture.scaleY = currentScale

                        // 关键：在0.8以下时开始自动渐变
                        if (currentScale <= 0.8f && !isAutoTransitioning && !transitionTriggered) {

                            transitionTriggered = true   //标志只执行一次

                            startSmoothTransition()
                        } else {
                            // 正常计算透明度
                            binding.ivPicture.alpha = currentScale
                        }

                        return true
                    }
                    return false
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    transitionTriggered = false
//                    if (isTransitioning || isAutoTransitioning) return
//                    if (currentScale <= 0.8f) {
//                        startSmoothTransition()
//                    } else if (currentScale < 1f) {
//                        resetImageView()
//                    }
                }
            })


        pictureGalleryAdapter.setOnItemClickListener(object : PictureGalleryAdapter.OnItemClickListener {

            override fun onItemClick(pictureList: List<Picture>, bean: Picture, position: Int) {

                Log.e("wwww", "position: $position")

                if(currentScrollerState == RecyclerView.SCROLL_STATE_IDLE){
                    binding.carouselRecyclerview.hide()
                    binding.ivPicture.show()
                    resetImageView()

                    currentPosition = binding.carouselRecyclerview.getSelectedPosition()
                    Glide.with(this@PictureActivity).load(pictureList[currentPosition].uri).into(binding.ivPicture)
                }




            }

        })


        binding.carouselRecyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                currentScrollerState = newState

                    when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        // 状态 0：滚动已停止（静止）
                        Log.e("wwww", "已停止滚动")
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        // 状态 1：用户手指正在拖拽（正在滚动）
                        Log.e("wwww", "用户正在拖拽滚动")
                    }
                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        // 状态 2：惯性滚动或自动对齐（正在滚动）
                        Log.e("wwww", "惯性滚动中")
                    }
                }
            }
        })



        binding.ivPicture.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun startSmoothTransition() {
        isAutoTransitioning = true

        // 计算剩余要缩小的比例
        val remainingScale = currentScale / 0.8f
        val duration = (300 * (1 - remainingScale)).toLong()

        // 平滑缩小到0
        binding.ivPicture.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.ivPicture.hide()
                    binding.ivPicture.scaleX = 1f
                    binding.ivPicture.scaleY = 1f
                    binding.ivPicture.alpha = 1f
                    isAutoTransitioning = false
                    isTransitioning = true

                    showCarouselWithAnimation()
                }
            })
            .start()
    }

    private fun showCarouselWithAnimation() {
        binding.carouselRecyclerview.apply {
            set3DItem(false)
            setAlpha(true)
            setInfinite(true)
            setIntervalRatio(0.352f)

            scaleX = 1.2f
            scaleY = 1.2f
            alpha = 0f
            show()

            pictureList = reorderList(pictureList, currentPosition)
            pictureGalleryAdapter.setData(pictureList)


            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(300)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        isTransitioning = false
                        currentScale = 1.0f
                    }
                })
                .start()
        }
    }

    private fun resetImageView() {
        binding.ivPicture.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentScale = 1.0f
                }
            })
            .start()
    }



    /**
     * 重新排序列表
     * 例如: [A,B,C,D,E], position=2 → [C,D,E,A,B]
     */
    private fun reorderList(originalList: List<Picture>, position: Int): ArrayList<Picture> {
        val newList = ArrayList<Picture>()

        // 从position开始到末尾
        for (i in position until originalList.size) {
            newList.add(originalList[i])
        }

        // 从0到position-1
        for (i in 0 until position) {
            newList.add(originalList[i])
        }

        return newList
    }


}