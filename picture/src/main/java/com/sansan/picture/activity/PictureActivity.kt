package com.sansan.picture.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sansan.picture.adapter.PictureGalleryAdapter
import com.sansan.picture.databinding.ActivityPictureBinding
import com.sansan.picture.model.Picture
import com.sansan.picture.utils.hide
import com.sansan.picture.utils.setFullScreen
import com.sansan.picture.utils.show
import com.sansan.picture.viewmodel.PictureViewModel
import androidx.core.view.isVisible
import com.sansan.picture.utils.toChineseDate
import com.sansan.picture.utils.toChineseTime
import com.sansan.picture.utils.toKB2Decimal
import com.sansan.picture.utils.wrapEvery


class PictureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPictureBinding

    private val viewModel: PictureViewModel by viewModels()

    private lateinit var pictureGalleryAdapter: PictureGalleryAdapter

    private lateinit var pictureList: ArrayList<Picture>
//    private lateinit var currentPicture: Picture
    private var currentPosition: Int = 0

    private var isGalleryShow = false   //是否显示画廊，用于只执行一次

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

        pictureGalleryAdapter.setOnItemClickListener(galleryItemClickListener)

        binding.carouselRecyclerview.apply {
            set3DItem(false)
            setAlpha(true)
            setInfinite(true)
            setIntervalRatio(0.352f)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {

                // 滚动状态变化
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    when (newState) {
                        RecyclerView.SCROLL_STATE_IDLE -> {       // 0: 静止
                            // 滚动停止，可恢复图片加载
                            binding.flPicture.setPictureCanTouch(true)
                        }
                        RecyclerView.SCROLL_STATE_DRAGGING -> {  // 1: 拖拽中
                            // 用户手指拖拽，可暂停图片加载
                        }
                        RecyclerView.SCROLL_STATE_SETTLING -> {  // 2: 惯性滚动
                            // 手指离开后的惯性滚动
                        }
                    }
                }

            })

        }



        binding.ivPicture.apply {
            setOnScaleChangeListener { scaleFactor, focusX, focusY ->
                val currentScale = scale

                if (currentScale < 0.7f && !isGalleryShow) {

                    Log.d("wwww", "当前缩放比例：$currentScale")

                    isGalleryShow = true

                    binding.ivPicture.hide()
                    binding.flPictureInfo.hide()
                    binding.carouselRecyclerview.show()

//                    pictureList = reorderList(pictureList, currentPosition)
//                    pictureGalleryAdapter.setData(pictureList)

                }

            }
        }


        binding.ivPicture.setOnClickListener {

            if(binding.flPictureInfo.isVisible){
                binding.flPictureInfo.hide()
            } else {
                binding.flPictureInfo.show()

                binding.tvPictureDate.text = pictureList[currentPosition].dateTaken.toChineseDate()
                binding.tvPictureTime.text = pictureList[currentPosition].dateTaken.toChineseTime()
                binding.tvPictureName.text = pictureList[currentPosition].name
                binding.tvPictureResolution.text = pictureList[currentPosition].width.toString() + "*" + pictureList[currentPosition].height.toString()
                binding.tvPictureSize.text = pictureList[currentPosition].size.toKB2Decimal()
                binding.tvPicturePath.text = "路径:" + pictureList[currentPosition].path.wrapEvery()

            }


        }

        binding.ivClose.setOnClickListener {
            finish()
        }



    }



    private val galleryItemClickListener = object : PictureGalleryAdapter.OnItemClickListener {
        override fun onItemClick(pictureList: List<Picture>, bean: Picture, position: Int) {
            currentPosition = position

            binding.carouselRecyclerview.hide()
            binding.ivPicture.show()
            isGalleryShow = false

            Glide.with(this@PictureActivity)
                .load(pictureList[currentPosition].uri)
                .into(binding.ivPicture)

            binding.flPicture.setPictureCanTouch(true)

        }
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