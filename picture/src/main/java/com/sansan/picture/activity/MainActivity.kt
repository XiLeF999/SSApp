package com.sansan.picture.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sansan.picture.R
import com.sansan.picture.adapter.PictureAdapter
import com.sansan.picture.adapter.PictureFolderAdapter
import com.sansan.picture.databinding.ActivityMainBinding
import com.sansan.picture.dialog.DeletePictureDialog
import com.sansan.picture.model.Picture
import com.sansan.picture.model.PictureFolder
import com.sansan.picture.repository.PictureRepository.StorageType
import com.sansan.picture.utils.hide
import com.sansan.picture.utils.setTransparentStatusBar
import com.sansan.picture.utils.show
import com.sansan.picture.viewmodel.MainViewModel
import com.sansan.picture.widget.PictureItemDecoration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.getValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    private val deletePictureDialog by lazy { DeletePictureDialog(this) }

    private lateinit var pictureFolderAdapter: PictureFolderAdapter
    private lateinit var pictureAdapter: PictureAdapter

    private var usbScanReceiver: BroadcastReceiver? = null
    // 定义列数配置
    private var columnCount = 5

    private var scrollbarArea: View? = null
    private var scrollbarThumb: View? = null

    // Keep the scroll metrics stable for the duration of a thumb drag. RecyclerView can
    // refine its estimated range while new date groups are laid out, which otherwise
    // makes the same thumb movement produce a different target on each MotionEvent.
    private var scrollbarDragMaxScroll = 0
    private var scrollbarDragMaxThumbOffset = 0f
    private var scrollbarDragTargetScroll = 0
    private var scrollbarDragStartY = 0f
    private var scrollbarDragStartThumbY = 0f
    private var isScrollbarDragging = false

    private fun isHalfScreen() = Resources.getSystem().configuration.screenWidthDp == 640

    private fun getPictureColumnCount(): Int =
        if (resources.getBoolean(R.bool.is_ky1045)) 3 else if (isHalfScreen()) 2 else 5


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTransparentStatusBar()
        scrollbarArea = findViewById(R.id.view_scrollbar_area)
        scrollbarThumb = findViewById(R.id.view_scrollbar_thumb)
        applyPictureAreaLayout(resources.configuration)

        registerUsbScanReceiver()

        pictureFolderAdapter = PictureFolderAdapter(this)
        pictureAdapter = PictureAdapter(this)

        // 根据配置决定列数
        columnCount = getPictureColumnCount()
        setupRecyclerViews()
        setupPictureScrollBar()


        viewModel.getPictureFolderList(StorageType.ALL)

        click()


        pictureFolderAdapter.setOnItemClickListener(object : PictureFolderAdapter.OnItemClickListener {
            override fun onItemClick(bean: PictureFolder, position: Int) {

                binding.rvFolderPicture.hide()
                binding.rvPicture.show()
                binding.rvPicture.post { updateCurrentPictureScrollBar() }
                binding.tvSelectNum.hide()

                viewModel.getPicturesInFolder(bean.id)

            }
        })


        pictureAdapter.setOnItemClickListener(object : PictureAdapter.OnItemClickListener {
            override fun onItemClick(pictureList: List<Picture>, bean: Picture, position: Int) {

                val intent = Intent(this@MainActivity, PictureActivity::class.java)

                // 传递整个列表
                intent.putExtra("picture_list", ArrayList(pictureList))
                // 传递当前点击的图片
                intent.putExtra("current_picture", bean)
                // 传递当前位置
                intent.putExtra("position", position)

                startActivity(intent)

            }
        })

        pictureAdapter.setOnSelectItemListener(object : PictureAdapter.OnSelectItemListener {
            override fun onSelectNum(num: Int) {

                binding.tvSelectNum.text = "已选中(${num})"

            }
        })


        deletePictureDialog.setOnPictureDialogClickListener(object : DeletePictureDialog.OnPictureDialogClickListener {
            override fun onSure() {

                val selectedPictures = pictureAdapter.getList().filter { it.isSelect }
                viewModel.deletePictures(selectedPictures)

            }
        })



        lifecycleScope.launch {
            //快速变化的用户操作，用 collectLatest，只显示最后一次结果。用了这个就不用手动cancel协程Job了
            viewModel.pictureFolderList.collectLatest { list ->
                pictureFolderAdapter.setData(list)
                binding.rvFolderPicture.post { updateCurrentPictureScrollBar() }

                if (list.isEmpty()) {
                    binding.rvFolderPicture.hide()
                    scrollbarArea?.hide()
                    binding.viewEmpty.show()

                } else {
                    binding.rvFolderPicture.show()
                    scrollbarArea?.show()
                    binding.viewEmpty.hide()
                }

            }
        }


        lifecycleScope.launch {
            //快速变化的用户操作，用 collectLatest，只显示最后一次结果。用了这个就不用手动cancel协程Job了
            viewModel.pictureList.collectLatest { list ->
                pictureAdapter.setData(list)
                binding.rvPicture.post { updateCurrentPictureScrollBar() }

                if (list.isEmpty()) {
                    binding.tvManager.hide()

                    binding.rvPicture.hide()
                    scrollbarArea?.hide()
                    binding.viewEmpty.show()

                } else {

                    binding.tvManager.show()

                    binding.rvPicture.show()
                    scrollbarArea?.show()
                    binding.viewEmpty.hide()

                }

                binding.tvSelectNum.text = "已选中(${list.count { it.isSelect }})"

            }
        }

        lifecycleScope.launch {
            //快速变化的用户操作，用 collectLatest，只显示最后一次结果。用了这个就不用手动cancel协程Job了
            viewModel.deletePictureCount.collectLatest { count ->

                if(count > 0) {
                    deletePictureDialog.deleteSuccess()
                    viewModel.getPicturesInFolder()

                } else {
                    deletePictureDialog.deleteFail()
                }

            }
        }



    }



    fun click() {

        binding.apply {

            tvAllPicture.setOnClickListener {

                tvAllPicture.isSelected = true
                tvUsbPicture.isSelected = false
                tvSdPicture.isSelected = false
                tvLocalPicture.isSelected = false

                tvAllPicture.textSize = 26f
                tvUsbPicture.textSize = 18f
                tvSdPicture.textSize = 18f
                tvLocalPicture.textSize = 18f

                rvFolderPicture.hide()
                rvPicture.hide()
                pictureAdapter.setData(emptyList())
                tvSelectNum.hide()
                pictureAdapter.setCancelManager()
                tvManager.hide()
                llManager.hide()
                viewModel.getPictureFolderList(StorageType.ALL)

            }

            tvUsbPicture.setOnClickListener {

                tvAllPicture.isSelected = false
                tvUsbPicture.isSelected = true
                tvSdPicture.isSelected = false
                tvLocalPicture.isSelected = false

                tvAllPicture.textSize = 18f
                tvUsbPicture.textSize = 26f
                tvSdPicture.textSize = 18f
                tvLocalPicture.textSize = 18f

                rvFolderPicture.hide()
                rvPicture.hide()
                pictureAdapter.setData(emptyList())
                tvSelectNum.hide()
                pictureAdapter.setCancelManager()
                tvManager.hide()
                llManager.hide()
                viewModel.getPictureFolderList(StorageType.USB)
            }

            tvSdPicture.setOnClickListener {

                tvAllPicture.isSelected = false
                tvUsbPicture.isSelected = false
                tvSdPicture.isSelected = true
                tvLocalPicture.isSelected = false

                tvAllPicture.textSize = 18f
                tvUsbPicture.textSize = 18f
                tvSdPicture.textSize = 26f
                tvLocalPicture.textSize = 18f

                rvFolderPicture.hide()
                rvPicture.hide()
                pictureAdapter.setData(emptyList())
                tvSelectNum.hide()
                pictureAdapter.setCancelManager()
                tvManager.hide()
                llManager.hide()
                viewModel.getPictureFolderList(StorageType.SD_CARD)
            }

            tvLocalPicture.setOnClickListener {

                tvAllPicture.isSelected = false
                tvUsbPicture.isSelected = false
                tvSdPicture.isSelected = false
                tvLocalPicture.isSelected = true

                tvAllPicture.textSize = 18f
                tvUsbPicture.textSize = 18f
                tvSdPicture.textSize = 18f
                tvLocalPicture.textSize = 26f

                rvFolderPicture.hide()
                rvPicture.hide()
                pictureAdapter.setData(emptyList())
                tvSelectNum.hide()
                pictureAdapter.setCancelManager()
                tvManager.hide()
                llManager.hide()
                viewModel.getPictureFolderList(StorageType.INTERNAL)
            }

            tvManager.setOnClickListener {

                tvManager.hide()
                llManager.show()
                tvSelectNum.show()
                pictureAdapter.setManager(true)


            }

            tvAllSelect.setOnClickListener {
                pictureAdapter.setAllSelect()
            }

            tvDelete.setOnClickListener {

                val selectCount = pictureAdapter.getSelectCount()
                if (selectCount <= 0){
                    Toast.makeText(this@MainActivity, "请选择图片", Toast.LENGTH_SHORT).show()
                } else {
                    deletePictureDialog.showDeleteDialog()
                }

            }

            tvCancel.setOnClickListener {
                tvManager.show()
                llManager.hide()
                binding.tvSelectNum.hide()
                pictureAdapter.setCancelManager()
            }

        }


    }


    override fun onStart() {
        super.onStart()
        onConfigurationChanged(resources.configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyPictureAreaLayout(newConfig)
        // 配置变化时更新列数
        columnCount = getPictureColumnCount()
        updateRecyclerViewColumns()
    }

    private fun applyPictureAreaLayout(configuration: Configuration) {
        if (!resources.getBoolean(R.bool.is_ky1045)) {
            binding.llMenu.layoutParams.width = if (configuration.screenWidthDp != 640) 420 else 300
            return
        }

        binding.llMenu.layoutParams = binding.llMenu.layoutParams.apply {
            width = resources.getDimensionPixelSize(R.dimen.picture_menu_width)
        }

        listOf(binding.rvFolderPicture, binding.rvPicture).forEach { recyclerView ->
            recyclerView.layoutParams = (recyclerView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                width = resources.getDimensionPixelSize(R.dimen.picture_list_width)
                marginStart = 0
                marginEnd = 0
            }
            recyclerView.setPadding(
                resources.getDimensionPixelSize(R.dimen.picture_list_inner_start),
                recyclerView.paddingTop,
                0,
                recyclerView.paddingBottom
            )
        }
    }
    private fun setupPictureScrollBar() {
        if (!resources.getBoolean(R.bool.is_ky1045)) {
            scrollbarArea?.hide()
            return
        }

        val listener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!isScrollbarDragging) {
                    updatePictureScrollBar(recyclerView)
                }
            }
        }

        listOf(binding.rvFolderPicture, binding.rvPicture).forEach { recyclerView ->
            recyclerView.isVerticalScrollBarEnabled = false
            recyclerView.addOnScrollListener(listener)
        }
        fun attachScrollbarTouchListener(view: View?, useRawY: Boolean) {
            view?.setOnTouchListener { _, event ->
                val area = scrollbarArea ?: return@setOnTouchListener false
                val thumb = scrollbarThumb ?: return@setOnTouchListener false
                val recyclerView = if (binding.rvPicture.visibility == View.VISIBLE) {
                    binding.rvPicture
                } else {
                    binding.rvFolderPicture
                }

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val maxScroll = recyclerView.computeVerticalScrollRange() -
                                recyclerView.computeVerticalScrollExtent()
                        val maxThumbOffset = area.height - thumb.height
                        if (maxScroll <= 0 || maxThumbOffset <= 0) return@setOnTouchListener true

                        scrollbarDragMaxScroll = maxScroll
                        scrollbarDragMaxThumbOffset = maxThumbOffset.toFloat()
                        scrollbarDragTargetScroll = recyclerView.computeVerticalScrollOffset()
                        scrollbarDragStartY = if (useRawY) event.rawY else event.y
                        scrollbarDragStartThumbY = thumb.translationY
                        isScrollbarDragging = true
                        // Bitmap decoding competes with RecyclerView layout exactly when
                        // a fast drag enters a new date group. Defer requests until release.
                        Glide.with(this@MainActivity).pauseRequests()
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (scrollbarDragMaxScroll <= 0 || scrollbarDragMaxThumbOffset <= 0f) {
                            return@setOnTouchListener true
                        }
                        val y = if (useRawY) event.rawY else event.y
                        val targetThumb = (
                                scrollbarDragStartThumbY + y - scrollbarDragStartY
                                ).coerceIn(0f, scrollbarDragMaxThumbOffset)
                        val targetScroll = (
                                targetThumb / scrollbarDragMaxThumbOffset * scrollbarDragMaxScroll
                                ).toInt()
                        val delta = targetScroll - scrollbarDragTargetScroll
                        if (delta != 0) {
                            recyclerView.scrollBy(0, delta)
                            scrollbarDragTargetScroll = targetScroll
                        }
                        // Keep the visual thumb under the user's finger while the list lays out.
                        thumb.translationY = targetThumb
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        scrollbarDragMaxScroll = 0
                        scrollbarDragMaxThumbOffset = 0f
                        if (isScrollbarDragging) {
                            isScrollbarDragging = false
                            Glide.with(this@MainActivity).resumeRequests()
                            recyclerView.post { updatePictureScrollBar(recyclerView) }
                        }
                        true
                    }

                    else -> true
                }
            }
        }

        attachScrollbarTouchListener(scrollbarArea, useRawY = false)
        attachScrollbarTouchListener(scrollbarThumb, useRawY = true)
        scrollbarArea?.post { updateCurrentPictureScrollBar() }
    }

    private fun updateCurrentPictureScrollBar() {
        val recyclerView = if (binding.rvPicture.visibility == View.VISIBLE) {
            binding.rvPicture
        } else {
            binding.rvFolderPicture
        }
        updatePictureScrollBar(recyclerView)
    }

    private fun updatePictureScrollBar(recyclerView: RecyclerView) {
        val scrollRange = recyclerView.computeVerticalScrollRange()
        val scrollExtent = recyclerView.computeVerticalScrollExtent()
        val maxScroll = scrollRange - scrollExtent
        val area = scrollbarArea ?: return
        val thumb = scrollbarThumb ?: return
        val maxThumbOffset = area.height - thumb.height

        thumb.translationY = if (maxScroll > 0 && maxThumbOffset > 0) {
            recyclerView.computeVerticalScrollOffset().toFloat() / maxScroll * maxThumbOffset
        } else {
            0f
        }
    }
    private fun setupRecyclerViews() {
        binding.apply {
            binding.tvAllPicture.isSelected = true

            setupRecyclerView(rvFolderPicture, columnCount)
            setupRecyclerView(rvPicture, columnCount)
        }
    }

    private fun setupRecyclerView(recyclerView: RecyclerView, spanCount: Int) {
        recyclerView.apply {
            setHasFixedSize(true)
            itemAnimator = null
            setItemViewCacheSize(6)
            layoutManager = GridLayoutManager(this@MainActivity, spanCount).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int =
                        if (recyclerView == binding.rvPicture && recyclerView.adapter?.getItemViewType(position) == 0) spanCount else 1
                }
            }
            adapter = if (recyclerView == binding.rvFolderPicture) {
                pictureFolderAdapter
            } else {
                pictureAdapter
            }

            // 移除旧的ItemDecoration
            repeat(itemDecorationCount) { index ->
                val decoration = getItemDecorationAt(0)
                if (decoration is PictureItemDecoration) {
                    removeItemDecoration(decoration)
                }
            }

            // 添加新的ItemDecoration
            val itemDecoration = PictureItemDecoration(
                spanCount = spanCount,
                spacingX = resources.getDimensionPixelSize(R.dimen.picture_grid_spacing_horizontal),
                spacingY = resources.getDimensionPixelSize(R.dimen.picture_grid_spacing_vertical),
                isKy1045 = resources.getBoolean(R.bool.is_ky1045)
            )
            addItemDecoration(itemDecoration)
        }
    }

    private fun updateRecyclerViewColumns() {
        // 更新两个RecyclerView的列数
        updateRecyclerViewLayout(binding.rvFolderPicture, columnCount)
        updateRecyclerViewLayout(binding.rvPicture, columnCount)
    }

    private fun updateRecyclerViewLayout(recyclerView: RecyclerView, spanCount: Int) {
        val gridLayoutManager = recyclerView.layoutManager as? GridLayoutManager
        gridLayoutManager?.apply {
            this.spanCount = spanCount
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int =
                    if (recyclerView.adapter?.getItemViewType(position) == 0 && recyclerView == binding.rvPicture) spanCount else 1
            }
        }

        // 更新ItemDecoration
        recyclerView.itemDecorationCount.let { count ->
            repeat(count) { index ->
                val decoration = recyclerView.getItemDecorationAt(index)
                if (decoration is PictureItemDecoration) {
                    // 重新创建Decoration
                    recyclerView.removeItemDecoration(decoration)
                    val newDecoration = PictureItemDecoration(
                        spanCount = spanCount,
                        spacingX = resources.getDimensionPixelSize(R.dimen.picture_grid_spacing_horizontal),
                        spacingY = resources.getDimensionPixelSize(R.dimen.picture_grid_spacing_vertical),
                isKy1045 = resources.getBoolean(R.bool.is_ky1045)
                    )
                    recyclerView.addItemDecoration(newDecoration)
                }
            }
        }
    }




    /**
     * 注册USB扫描广播接收器
     */
    private fun registerUsbScanReceiver() {
        if (usbScanReceiver == null) {
            usbScanReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        Intent.ACTION_MEDIA_SCANNER_STARTED -> {
                            Log.e("wwww","系统开始扫描媒体库...")
                        }

                        Intent.ACTION_MEDIA_SCANNER_FINISHED -> {
                            Log.e("wwww","系统媒体库扫描完成")
                            viewModel.getPictureFolderList()
                            viewModel.getPicturesInFolder()
                        }

                        Intent.ACTION_MEDIA_UNMOUNTED,
                        Intent.ACTION_MEDIA_EJECT,
                        Intent.ACTION_MEDIA_BAD_REMOVAL -> {
                            Log.e("wwww","USB设备已拔出")
                            viewModel.getPictureFolderList()
                            viewModel.getPicturesInFolder()
                        }

                    }
                }
            }

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_SCANNER_STARTED)
                addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED)
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addAction(Intent.ACTION_MEDIA_EJECT)
                addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
                addDataScheme("file")
            }

            registerReceiver(usbScanReceiver, filter)
        }
    }




}












