package com.sansan.picture.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.sansan.picture.adapter.PictureGalleryAdapter
import com.sansan.picture.databinding.ActivityPictureBinding
import com.sansan.picture.model.Picture
import com.sansan.picture.utils.hide
import com.sansan.picture.utils.setFullScreen
import com.sansan.picture.utils.show
import com.sansan.picture.viewmodel.PictureViewModel


class PictureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPictureBinding

    private val viewModel: PictureViewModel by viewModels()

    private lateinit var pictureGalleryAdapter: PictureGalleryAdapter

    private lateinit var pictureList: ArrayList<Picture>
//    private lateinit var currentPicture: Picture
    private var currentPosition: Int = 0

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


        binding.ivPicture.apply {
            setOnScaleChangeListener { scaleFactor, focusX, focusY ->
                val currentScale = scale
                Log.d("wwww", "当前缩放比例：$currentScale")

                if (currentScale <= 0.8f) {

                    binding.ivPicture.hide()
                    binding.carouselRecyclerview.apply {
                        set3DItem(false)
                        setAlpha(true)
                        setInfinite(true)
                        setIntervalRatio(0.352f)

                        show()

                        pictureList = reorderList(pictureList, currentPosition)
                        pictureGalleryAdapter.setData(pictureList)

                    }


                }

            }
        }

    }



    private val galleryItemClickListener = object : PictureGalleryAdapter.OnItemClickListener {
        override fun onItemClick(pictureList: List<Picture>, bean: Picture, position: Int) {
            currentPosition = position

            binding.carouselRecyclerview.hide()
            binding.ivPicture.show()

            Glide.with(this@PictureActivity)
                .load(pictureList[currentPosition].uri)
                .into(binding.ivPicture)

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