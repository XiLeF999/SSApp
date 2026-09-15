package com.sansan.picture.activity

import android.os.Bundle
import android.view.View
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.sansan.picture.R
import com.sansan.picture.adapter.PictureGalleryAdapter
import com.sansan.picture.databinding.ActivityPictureBinding
import com.sansan.picture.model.Picture
import com.sansan.picture.utils.hide
import com.sansan.picture.utils.setFullScreen
import com.sansan.picture.utils.show
import com.sansan.picture.viewmodel.PictureViewModel
import com.sansan.picture.adapter.PhotoAdapter
import com.sansan.picture.utils.toChineseDate
import com.sansan.picture.utils.toChineseTime
import com.sansan.picture.utils.toKB2Decimal
import com.sansan.picture.utils.truncateMiddle
import com.sansan.picture.utils.wrapEvery


class PictureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPictureBinding

    private val viewModel: PictureViewModel by viewModels()

    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var pictureGalleryAdapter: PictureGalleryAdapter

    private lateinit var pictureList: ArrayList<Picture>

    private var currentPosition: Int = 0
    private val isKy1045 by lazy { resources.getBoolean(R.bool.is_ky1045) }

    private var isGalleryShow = false
    private var systemBarsJob: Job? = null
    private var pictureInfoJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFullScreen()

        pictureList = intent.getSerializableExtra("picture_list") as ArrayList<Picture>
        val pos = intent.getIntExtra("position", 0)

        currentPosition = pos


photoAdapter = PhotoAdapter(this).apply {
            setData(pictureList)
            onShowGallery = { showGallery() }
            onTogglePhotoInfo = { togglePictureInfo() }
            onSwipeUp = { showDeleteDialog() }
            onSwipeDown = { showSystemBarsTemporarily() }
        }

        binding.vpPicture.apply {
            setPageTransformer(com.sansan.picture.widget.PictureGalleryTransformer())
            offscreenPageLimit = 2
            adapter = photoAdapter
            setCurrentItem(currentPosition, false)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    currentPosition = position

                    if (binding.flPictureInfo.isVisible) {
                        bindPictureInfo(pictureList[currentPosition])
                        schedulePictureInfoHide()
                    }
                }
            })
        }


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

        binding.ivClose.setOnClickListener { finish() }

    }

    private fun bindPictureInfo(picture: Picture) {
        binding.run {
            tvPictureDate.text = picture.dateTaken.toChineseDate()
            tvPictureTime.text = picture.dateTaken.toChineseTime()
            tvPictureName.text = picture.name.truncateMiddle()
            tvPictureResolution.text = picture.width.toString() + "x" + picture.height.toString()
            tvPictureSize.text = picture.size.toKB2Decimal()
            tvPicturePath.text = ("路径:" + picture.path).wrapEvery()
        }
    }


    private fun togglePictureInfo() {
        if (binding.flPictureInfo.isVisible) {
            hidePictureInfo()
        } else {
            binding.flPictureInfo.show()
            bindPictureInfo(pictureList[currentPosition])
            schedulePictureInfoHide()
        }
    }

    private fun schedulePictureInfoHide() {
        pictureInfoJob?.cancel()
        pictureInfoJob = lifecycleScope.launch {
            delay(2500)
            binding.flPictureInfo.hide()
        }
    }

    private fun hidePictureInfo() {
        pictureInfoJob?.cancel()
        binding.flPictureInfo.hide()
    }

    private fun showGallery() {
        binding.flPictureInfo.hide()
        binding.flPicture.hide()
        binding.carouselRecyclerview.show()
        binding.carouselRecyclerview.getCarouselLayoutManager().scrollToPosition(currentPosition)
        isGalleryShow = true
    }

    private fun showDeleteDialog() {
        val dialog = com.sansan.picture.dialog.DeletePictureDialog(this)
        dialog.setOnPictureDialogClickListener(object : com.sansan.picture.dialog.DeletePictureDialog.OnPictureDialogClickListener {
            override fun onSure() {
                viewModel.deletePicture(pictureList[currentPosition]) { success ->
                    if (success) {
                        dialog.dismiss()
                        pictureList.removeAt(currentPosition)
                        if (pictureList.isEmpty()) finish() else {
                            currentPosition = currentPosition.coerceAtMost(pictureList.lastIndex)
                            photoAdapter.setData(pictureList)
                            pictureGalleryAdapter.setData(pictureList)
                            binding.vpPicture.setCurrentItem(currentPosition, false)
                        }
                    } else dialog.deleteFail()
                }
            }
        })
        dialog.showDeleteDialog()
    }

    private fun showSystemBarsTemporarily() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        systemBarsJob?.cancel()
        systemBarsJob = lifecycleScope.launch {
            delay(2500)
            setFullScreen()
        }
    }

    private val galleryItemClickListener = object : PictureGalleryAdapter.OnItemClickListener {
        override fun onItemClick(pictureList: List<Picture>, bean: Picture, position: Int) {
            currentPosition = position

            binding.carouselRecyclerview.hide()
            binding.flPicture.show()
            isGalleryShow = false

            binding.flPicture.setPictureCanTouch(true)

            binding.vpPicture.setCurrentItem(currentPosition, false)
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
