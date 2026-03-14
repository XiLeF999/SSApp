package com.sansan.picture.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.sansan.picture.adapter.PictureAdapter
import com.sansan.picture.adapter.PictureFolderAdapter
import com.sansan.picture.databinding.ActivityMainBinding
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

    private lateinit var pictureFolderAdapter: PictureFolderAdapter
    private lateinit var pictureAdapter: PictureAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTransparentStatusBar()

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
                    spacingY = 8
                )
                addItemDecoration(itemDecoration)
            }
            rvPicture.apply {
                layoutManager = GridLayoutManager(this@MainActivity, 5)
                adapter = pictureAdapter

                val itemDecoration = PictureItemDecoration(
                    spanCount = 5,
                    spacingX = 0,
                    spacingY = 8
                )
                addItemDecoration(itemDecoration)
            }
        }

        viewModel.getPictureFolderList(StorageType.ALL)

        click()


        pictureFolderAdapter.setOnItemClickListener(object : PictureFolderAdapter.OnItemClickListener {
            override fun onItemClick(bean: PictureFolder, position: Int) {

                binding.rvFolderPicture.hide()
                binding.rvPicture.show()

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



        lifecycleScope.launch {
            //快速变化的用户操作，用 collectLatest，只显示最后一次结果。用了这个就不用手动cancel协程Job了
            viewModel.pictureFolderList.collectLatest { list ->
                pictureFolderAdapter.setData(list)
            }
        }


        lifecycleScope.launch {
            //快速变化的用户操作，用 collectLatest，只显示最后一次结果。用了这个就不用手动cancel协程Job了
            viewModel.pictureList.collectLatest { list ->
                pictureAdapter.setData(list)

                if (list.isEmpty()) {
                    binding.tvManager.hide()

                } else {

                    binding.tvManager.show()

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

                rvFolderPicture.show()
                rvPicture.hide()
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

                rvFolderPicture.show()
                rvPicture.hide()
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

                rvFolderPicture.show()
                rvPicture.hide()
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

                rvFolderPicture.show()
                rvPicture.hide()
                pictureAdapter.setCancelManager()
                tvManager.hide()
                llManager.hide()
                viewModel.getPictureFolderList(StorageType.INTERNAL)
            }

            tvManager.setOnClickListener {

                tvManager.hide()
                llManager.show()

                pictureAdapter.setManager(true)


            }

            tvAllSelect.setOnClickListener {
                pictureAdapter.setAllSelect()
            }

            tvDelete.setOnClickListener {


            }

            tvCancel.setOnClickListener {
                tvManager.show()
                llManager.hide()
                pictureAdapter.setCancelManager()
            }

        }


    }




}