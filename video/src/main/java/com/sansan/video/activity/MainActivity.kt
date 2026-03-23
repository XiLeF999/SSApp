package com.sansan.video.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.sansan.video.adapter.VideoAdapter
import com.sansan.video.adapter.VideoFolderAdapter
import com.sansan.video.databinding.ActivityMainBinding
import com.sansan.video.dialog.DeleteVideoDialog
import com.sansan.video.model.Video
import com.sansan.video.model.VideoFolder
import com.sansan.video.repository.VideoRepository
import com.sansan.video.utils.hide
import com.sansan.video.utils.setTransparentStatusBar
import com.sansan.video.utils.show
import com.sansan.video.viewmodel.MainViewModel
import com.sansan.video.widget.VideoItemDecoration
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.collections.filter
import kotlin.getValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    private val deleteVideoDialog by lazy { DeleteVideoDialog(this) }

    private lateinit var videoFolderAdapter: VideoFolderAdapter
    private lateinit var videoAdapter: VideoAdapter

    private var usbScanReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTransparentStatusBar()

        registerUsbScanReceiver()

        videoFolderAdapter = VideoFolderAdapter(this)
        videoAdapter = VideoAdapter(this)

        binding.apply {

            binding.tvAllVideo.isSelected = true

            rvFolderVideo.apply {
                layoutManager = GridLayoutManager(this@MainActivity, 5)
                adapter = videoFolderAdapter

                val itemDecoration = VideoItemDecoration(
                    spanCount = 5,
                    spacingX = 0,
                    spacingY = 12
                )
                addItemDecoration(itemDecoration)
            }
            rvVideo.apply {
                layoutManager = GridLayoutManager(this@MainActivity, 5)
                adapter = videoAdapter

                val itemDecoration = VideoItemDecoration(
                    spanCount = 5,
                    spacingX = 0,
                    spacingY = 12
                )
                addItemDecoration(itemDecoration)
            }
        }

        viewModel.getVideoFolderList(VideoRepository.StorageType.ALL)

        click()


        videoFolderAdapter.setOnItemClickListener(object : VideoFolderAdapter.OnItemClickListener {
            override fun onItemClick(bean: VideoFolder, position: Int) {

                binding.rvFolderVideo.hide()
                binding.rvVideo.show()
                binding.tvSelectNum.hide()

                viewModel.getVideosInFolder(bean.id)

            }
        })


        videoAdapter.setOnItemClickListener(object : VideoAdapter.OnItemClickListener {
            override fun onItemClick(videoList: List<Video>, bean: Video, position: Int) {

                val intent = Intent(this@MainActivity, VideoActivity::class.java)

                // 传递当前点击的图片
                intent.putExtra("current_video", bean)

                startActivity(intent)

            }
        })

        videoAdapter.setOnSelectItemListener(object : VideoAdapter.OnSelectItemListener {
            override fun onSelectNum(num: Int) {

                binding.tvSelectNum.text = "已选中($num)"

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

                if (list.isEmpty()) {
                    binding.rvFolderVideo.hide()
                    binding.viewEmpty.show()

                } else {
                    binding.rvFolderVideo.show()
                    binding.viewEmpty.hide()
                }

            }
        }


        lifecycleScope.launch {
            //快速变化的用户操作，用 collectLatest，只显示最后一次结果。用了这个就不用手动cancel协程Job了
            viewModel.videoList.collectLatest { list ->
                videoAdapter.setData(list)

                if (list.isEmpty()) {
                    binding.tvManager.hide()

                    binding.rvVideo.hide()
                    binding.viewEmpty.show()

                } else {

                    binding.tvManager.show()

                    binding.rvVideo.show()
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
                viewModel.getVideoFolderList(VideoRepository.StorageType.ALL)

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
                viewModel.getVideoFolderList(VideoRepository.StorageType.USB)
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
                viewModel.getVideoFolderList(VideoRepository.StorageType.SD_CARD)
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
                viewModel.getVideoFolderList(VideoRepository.StorageType.INTERNAL)
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
                    Toast.makeText(this@MainActivity, "请选择图片", Toast.LENGTH_SHORT).show()
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