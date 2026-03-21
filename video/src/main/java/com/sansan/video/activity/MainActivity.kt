package com.sansan.video.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.sansan.video.adapter.PictureAdapter
import com.sansan.video.adapter.PictureFolderAdapter
import com.sansan.video.databinding.ActivityMainBinding
import com.sansan.video.dialog.DeletePictureDialog
import com.sansan.video.model.Picture
import com.sansan.video.model.PictureFolder
import com.sansan.video.repository.VideoRepository
import com.sansan.video.utils.hide
import com.sansan.video.utils.setTransparentStatusBar
import com.sansan.video.utils.show
import com.sansan.video.viewmodel.MainViewModel
import com.sansan.video.widget.PictureItemDecoration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.collections.filter
import kotlin.getValue
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    private val deletePictureDialog by lazy { DeletePictureDialog(this) }

    private lateinit var pictureFolderAdapter: PictureFolderAdapter
    private lateinit var pictureAdapter: PictureAdapter

    private var usbScanReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTransparentStatusBar()

        registerUsbScanReceiver()

        pictureFolderAdapter = PictureFolderAdapter(this)
        pictureAdapter = PictureAdapter(this)

        binding.apply {

            binding.tvAllPicture.isSelected = true

            rvFolderPicture.apply {
                layoutManager = GridLayoutManager(this@MainActivity, 5)
                adapter = pictureFolderAdapter

                val itemDecoration = PictureItemDecoration(
                    spanCount = 5,
                    spacingX = 0,
                    spacingY = 12
                )
                addItemDecoration(itemDecoration)
            }
            rvPicture.apply {
                layoutManager = GridLayoutManager(this@MainActivity, 5)
                adapter = pictureAdapter

                val itemDecoration = PictureItemDecoration(
                    spanCount = 5,
                    spacingX = 0,
                    spacingY = 12
                )
                addItemDecoration(itemDecoration)
            }
        }

        viewModel.getPictureFolderList(VideoRepository.StorageType.ALL)

        click()


        pictureFolderAdapter.setOnItemClickListener(object : PictureFolderAdapter.OnItemClickListener {
            override fun onItemClick(bean: PictureFolder, position: Int) {

                binding.rvFolderPicture.hide()
                binding.rvPicture.show()
                binding.tvSelectNum.hide()

                viewModel.getPicturesInFolder(bean.id)

            }
        })


        pictureAdapter.setOnItemClickListener(object : PictureAdapter.OnItemClickListener {
            override fun onItemClick(pictureList: List<Picture>, bean: Picture, position: Int) {

//                val intent = Intent(this@MainActivity, PictureActivity::class.java)
//
//                // 传递整个列表
//                intent.putExtra("picture_list", ArrayList(pictureList))
//                // 传递当前点击的图片
//                intent.putExtra("current_picture", bean)
//                // 传递当前位置
//                intent.putExtra("position", position)
//
//                startActivity(intent)

            }
        })

        pictureAdapter.setOnSelectItemListener(object : PictureAdapter.OnSelectItemListener {
            override fun onSelectNum(num: Int) {

                binding.tvSelectNum.text = "已选中($num)"

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

                if (list.isEmpty()) {
                    binding.rvFolderPicture.hide()
                    binding.viewEmpty.show()

                } else {
                    binding.rvFolderPicture.show()
                    binding.viewEmpty.hide()
                }

            }
        }


        lifecycleScope.launch {
            //快速变化的用户操作，用 collectLatest，只显示最后一次结果。用了这个就不用手动cancel协程Job了
            viewModel.pictureList.collectLatest { list ->
                pictureAdapter.setData(list)

                if (list.isEmpty()) {
                    binding.tvManager.hide()

                    binding.rvPicture.hide()
                    binding.viewEmpty.show()

                } else {

                    binding.tvManager.show()

                    binding.rvPicture.show()
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
                viewModel.getPictureFolderList(VideoRepository.StorageType.ALL)

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
                viewModel.getPictureFolderList(VideoRepository.StorageType.USB)
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
                viewModel.getPictureFolderList(VideoRepository.StorageType.SD_CARD)
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
                viewModel.getPictureFolderList(VideoRepository.StorageType.INTERNAL)
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