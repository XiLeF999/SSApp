package com.sansan.video.activity

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
import com.sansan.video.R
import com.sansan.video.adapter.VideoAdapter
import com.sansan.video.adapter.VideoFolderAdapter
import com.sansan.video.databinding.ActivityMainBinding
import com.sansan.video.dialog.DeleteVideoDialog
import com.sansan.video.model.Video
import com.sansan.video.model.VideoFolder
import com.sansan.video.repository.VideoRepository.StorageType
import com.sansan.video.utils.hide
import com.sansan.video.utils.setTransparentStatusBar
import com.sansan.video.utils.show
import com.sansan.video.viewmodel.MainViewModel
import com.sansan.video.widget.VideoItemDecoration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.getValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    private val deleteVideoDialog by lazy { DeleteVideoDialog(this) }

    private lateinit var videoFolderAdapter: VideoFolderAdapter
    private lateinit var videoAdapter: VideoAdapter

    private var usbScanReceiver: BroadcastReceiver? = null
    // 定义列数配置
    private var columnCount = 5

    private var scrollbarArea: View? = null
    private var scrollbarThumb: View? = null

    // Keep the scroll metrics stable while dragging. RecyclerView refines its estimated
    // range as new date groups are laid out, which otherwise makes the thumb feel stuck.
    private var scrollbarDragMaxScroll = 0
    private var scrollbarDragMaxThumbOffset = 0f
    private var scrollbarDragTargetScroll = 0
    private var scrollbarDragStartY = 0f
    private var scrollbarDragStartThumbY = 0f
    private var isScrollbarDragging = false

    private fun isHalfScreen() = Resources.getSystem().configuration.screenWidthDp == 640

    private fun getVideoColumnCount(): Int =
        if (resources.getBoolean(R.bool.is_ky1045)) 3 else if (isHalfScreen()) 2 else 5


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTransparentStatusBar()
        scrollbarArea = findViewById(R.id.view_scrollbar_area)
        scrollbarThumb = findViewById(R.id.view_scrollbar_thumb)
        applyVideoAreaLayout(resources.configuration)

        registerUsbScanReceiver()

        videoFolderAdapter = VideoFolderAdapter(this)
        videoAdapter = VideoAdapter(this)

        // 根据配置决定列数
        columnCount = getVideoColumnCount()
        setupRecyclerViews()
        setupVideoScrollBar()


        viewModel.getVideoFolderList(StorageType.ALL)

        click()


        videoFolderAdapter.setOnItemClickListener(object : VideoFolderAdapter.OnItemClickListener {
            override fun onItemClick(bean: VideoFolder, position: Int) {

                binding.rvFolderVideo.hide()
                binding.rvVideo.show()
                binding.rvVideo.post { updateCurrentVideoScrollBar() }
                binding.tvSelectNum.hide()

                viewModel.getVideosInFolder(bean.id)

            }
        })


        videoAdapter.setOnItemClickListener(object : VideoAdapter.OnItemClickListener {
            override fun onItemClick(videoList: List<Video>, bean: Video, position: Int) {

                val intent = Intent(this@MainActivity, VideoActivity::class.java)

                // 传递整个列表
                intent.putExtra("video_list", ArrayList(videoList))
                // 传递当前点击的视频
                intent.putExtra("current_video", bean)
                // 传递当前位置
                intent.putExtra("position", position)

                startActivity(intent)

            }
        })

        videoAdapter.setOnSelectItemListener(object : VideoAdapter.OnSelectItemListener {
            override fun onSelectNum(num: Int) {

                binding.tvSelectNum.text = "已选中(${num})"

            }
        })


        deleteVideoDialog.setOnVideoDialogClickListener(object : DeleteVideoDialog.OnVideoDialogClickListener {
            override fun onSure() {

                val selectedVideos = videoAdapter.getList().filter { it.isSelect }
                viewModel.deleteVideos(selectedVideos)

            }
        })



        lifecycleScope.launch {
            //快速变化的用户操作，用 collectLatest，只显示最后一次结果。用了这个就不用手动cancel协程Job了
            viewModel.videoFolderList.collectLatest { list ->
                videoFolderAdapter.setData(list)
                binding.rvFolderVideo.post { updateCurrentVideoScrollBar() }

                if (list.isEmpty()) {
                    binding.rvFolderVideo.hide()
                    scrollbarArea?.hide()
                    binding.viewEmpty.show()

                } else {
                    binding.rvFolderVideo.show()
                    scrollbarArea?.show()
                    binding.viewEmpty.hide()
                }

            }
        }


        lifecycleScope.launch {
            //快速变化的用户操作，用 collectLatest，只显示最后一次结果。用了这个就不用手动cancel协程Job了
            viewModel.videoList.collectLatest { list ->
                videoAdapter.setData(list)
                binding.rvVideo.post { updateCurrentVideoScrollBar() }

                if (list.isEmpty()) {
                    binding.tvManager.hide()

                    binding.rvVideo.hide()
                    scrollbarArea?.hide()
                    binding.viewEmpty.show()

                } else {

                    binding.tvManager.show()

                    binding.rvVideo.show()
                    scrollbarArea?.show()
                    binding.viewEmpty.hide()

                }

                binding.tvSelectNum.text = "已选中(${list.count { it.isSelect }})"

            }
        }

        lifecycleScope.launch {
            //快速变化的用户操作，用 collectLatest，只显示最后一次结果。用了这个就不用手动cancel协程Job了
            viewModel.deleteVideoCount.collectLatest { count ->

                if(count > 0) {
                    deleteVideoDialog.deleteSuccess()
                    viewModel.getVideosInFolder()

                } else {
                    deleteVideoDialog.deleteFail()
                }

            }
        }



    }



    fun click() {

        binding.apply {

            tvAllVideo.setOnClickListener {

                tvAllVideo.isSelected = true
                tvUsbVideo.isSelected = false
                tvSdVideo.isSelected = false
                tvLocalVideo.isSelected = false

                tvAllVideo.textSize = 26f
                tvUsbVideo.textSize = 18f
                tvSdVideo.textSize = 18f
                tvLocalVideo.textSize = 18f

                rvFolderVideo.hide()
                rvVideo.hide()
                videoAdapter.setData(emptyList())
                tvSelectNum.hide()
                videoAdapter.setCancelManager()
                tvManager.hide()
                llManager.hide()
                viewModel.getVideoFolderList(StorageType.ALL)

            }

            tvUsbVideo.setOnClickListener {

                tvAllVideo.isSelected = false
                tvUsbVideo.isSelected = true
                tvSdVideo.isSelected = false
                tvLocalVideo.isSelected = false

                tvAllVideo.textSize = 18f
                tvUsbVideo.textSize = 26f
                tvSdVideo.textSize = 18f
                tvLocalVideo.textSize = 18f

                rvFolderVideo.hide()
                rvVideo.hide()
                videoAdapter.setData(emptyList())
                tvSelectNum.hide()
                videoAdapter.setCancelManager()
                tvManager.hide()
                llManager.hide()
                viewModel.getVideoFolderList(StorageType.USB)
            }

            tvSdVideo.setOnClickListener {

                tvAllVideo.isSelected = false
                tvUsbVideo.isSelected = false
                tvSdVideo.isSelected = true
                tvLocalVideo.isSelected = false

                tvAllVideo.textSize = 18f
                tvUsbVideo.textSize = 18f
                tvSdVideo.textSize = 26f
                tvLocalVideo.textSize = 18f

                rvFolderVideo.hide()
                rvVideo.hide()
                videoAdapter.setData(emptyList())
                tvSelectNum.hide()
                videoAdapter.setCancelManager()
                tvManager.hide()
                llManager.hide()
                viewModel.getVideoFolderList(StorageType.SD_CARD)
            }

            tvLocalVideo.setOnClickListener {

                tvAllVideo.isSelected = false
                tvUsbVideo.isSelected = false
                tvSdVideo.isSelected = false
                tvLocalVideo.isSelected = true

                tvAllVideo.textSize = 18f
                tvUsbVideo.textSize = 18f
                tvSdVideo.textSize = 18f
                tvLocalVideo.textSize = 26f

                rvFolderVideo.hide()
                rvVideo.hide()
                videoAdapter.setData(emptyList())
                tvSelectNum.hide()
                videoAdapter.setCancelManager()
                tvManager.hide()
                llManager.hide()
                viewModel.getVideoFolderList(StorageType.INTERNAL)
            }

            tvManager.setOnClickListener {

                tvManager.hide()
                llManager.show()
                tvSelectNum.show()
                videoAdapter.setManager(true)


            }

            tvAllSelect.setOnClickListener {
                videoAdapter.setAllSelect()
            }

            tvDelete.setOnClickListener {

                val selectCount = videoAdapter.getSelectCount()
                if (selectCount <= 0){
                    Toast.makeText(this@MainActivity, "请选择视频", Toast.LENGTH_SHORT).show()
                } else {
                    deleteVideoDialog.showDeleteDialog()
                }

            }

            tvCancel.setOnClickListener {
                tvManager.show()
                llManager.hide()
                binding.tvSelectNum.hide()
                videoAdapter.setCancelManager()
            }

        }


    }


    override fun onStart() {
        super.onStart()
        onConfigurationChanged(resources.configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyVideoAreaLayout(newConfig)
        // 配置变化时更新列数
        columnCount = getVideoColumnCount()
        updateRecyclerViewColumns()
    }

    private fun applyVideoAreaLayout(configuration: Configuration) {
        if (!resources.getBoolean(R.bool.is_ky1045)) {
            binding.llMenu.layoutParams.width = if (configuration.screenWidthDp != 640) 420 else 300
            return
        }

        binding.llMenu.layoutParams = binding.llMenu.layoutParams.apply {
            width = resources.getDimensionPixelSize(R.dimen.video_menu_width)
        }

        listOf(binding.rvFolderVideo, binding.rvVideo).forEach { recyclerView ->
            recyclerView.layoutParams = (recyclerView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                width = resources.getDimensionPixelSize(R.dimen.video_list_width)
                marginStart = 0
                marginEnd = 0
            }
            recyclerView.setPadding(
                resources.getDimensionPixelSize(R.dimen.video_list_inner_start),
                recyclerView.paddingTop,
                0,
                recyclerView.paddingBottom
            )
        }
    }
    private fun setupVideoScrollBar() {
        if (!resources.getBoolean(R.bool.is_ky1045)) {
            scrollbarArea?.hide()
            return
        }

        val listener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!isScrollbarDragging) {
                    updateVideoScrollBar(recyclerView)
                }
            }
        }

        listOf(binding.rvFolderVideo, binding.rvVideo).forEach { recyclerView ->
            recyclerView.isVerticalScrollBarEnabled = false
            recyclerView.addOnScrollListener(listener)
        }
        fun attachScrollbarTouchListener(view: View?, useRawY: Boolean) {
            view?.setOnTouchListener { _, event ->
                val area = scrollbarArea ?: return@setOnTouchListener false
                val thumb = scrollbarThumb ?: return@setOnTouchListener false
                val recyclerView = if (binding.rvVideo.visibility == View.VISIBLE) {
                    binding.rvVideo
                } else {
                    binding.rvFolderVideo
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
                        // Avoid thumbnail decoding competing with layout at date boundaries.
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
                        thumb.translationY = targetThumb
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        scrollbarDragMaxScroll = 0
                        scrollbarDragMaxThumbOffset = 0f
                        if (isScrollbarDragging) {
                            isScrollbarDragging = false
                            Glide.with(this@MainActivity).resumeRequests()
                            recyclerView.post { updateVideoScrollBar(recyclerView) }
                        }
                        true
                    }

                    else -> true
                }
            }
        }

        attachScrollbarTouchListener(scrollbarArea, useRawY = false)
        attachScrollbarTouchListener(scrollbarThumb, useRawY = true)
        scrollbarArea?.post { updateCurrentVideoScrollBar() }
    }

    private fun updateCurrentVideoScrollBar() {
        val recyclerView = if (binding.rvVideo.visibility == View.VISIBLE) {
            binding.rvVideo
        } else {
            binding.rvFolderVideo
        }
        updateVideoScrollBar(recyclerView)
    }

    private fun updateVideoScrollBar(recyclerView: RecyclerView) {
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
            binding.tvAllVideo.isSelected = true

            setupRecyclerView(rvFolderVideo, columnCount)
            setupRecyclerView(rvVideo, columnCount)
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
                        if (recyclerView == binding.rvVideo && recyclerView.adapter?.getItemViewType(position) == 0) spanCount else 1
                }
            }
            adapter = if (recyclerView == binding.rvFolderVideo) {
                videoFolderAdapter
            } else {
                videoAdapter
            }

            // 移除旧的ItemDecoration
            repeat(itemDecorationCount) { index ->
                val decoration = getItemDecorationAt(0)
                if (decoration is VideoItemDecoration) {
                    removeItemDecoration(decoration)
                }
            }

            // 添加新的ItemDecoration
            val itemDecoration = VideoItemDecoration(
                spanCount = spanCount,
                spacingX = resources.getDimensionPixelSize(R.dimen.video_grid_spacing_horizontal),
                spacingY = resources.getDimensionPixelSize(R.dimen.video_grid_spacing_vertical),
                isKy1045 = resources.getBoolean(R.bool.is_ky1045)
            )
            addItemDecoration(itemDecoration)
        }
    }

    private fun updateRecyclerViewColumns() {
        // 更新两个RecyclerView的列数
        updateRecyclerViewLayout(binding.rvFolderVideo, columnCount)
        updateRecyclerViewLayout(binding.rvVideo, columnCount)
    }

    private fun updateRecyclerViewLayout(recyclerView: RecyclerView, spanCount: Int) {
        val gridLayoutManager = recyclerView.layoutManager as? GridLayoutManager
        gridLayoutManager?.apply {
            this.spanCount = spanCount
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int =
                    if (recyclerView.adapter?.getItemViewType(position) == 0 && recyclerView == binding.rvVideo) spanCount else 1
            }
        }

        // 更新ItemDecoration
        recyclerView.itemDecorationCount.let { count ->
            repeat(count) { index ->
                val decoration = recyclerView.getItemDecorationAt(index)
                if (decoration is VideoItemDecoration) {
                    // 重新创建Decoration
                    recyclerView.removeItemDecoration(decoration)
                    val newDecoration = VideoItemDecoration(
                        spanCount = spanCount,
                        spacingX = resources.getDimensionPixelSize(R.dimen.video_grid_spacing_horizontal),
                        spacingY = resources.getDimensionPixelSize(R.dimen.video_grid_spacing_vertical),
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
                            viewModel.getVideoFolderList()
                            viewModel.getVideosInFolder()
                        }

                        Intent.ACTION_MEDIA_UNMOUNTED,
                        Intent.ACTION_MEDIA_EJECT,
                        Intent.ACTION_MEDIA_BAD_REMOVAL -> {
                            Log.e("wwww","USB设备已拔出")
                            viewModel.getVideoFolderList()
                            viewModel.getVideosInFolder()
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













