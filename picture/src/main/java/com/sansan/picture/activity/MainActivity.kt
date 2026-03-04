package com.sansan.picture.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.sansan.picture.adapter.PictureAdapter
import com.sansan.picture.databinding.ActivityMainBinding
import com.sansan.picture.utils.setTransparentStatusBar
import com.sansan.picture.viewmodel.MainViewModel
import com.sansan.picture.widget.PictureItemDecoration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.getValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    private lateinit var pictureAdapter: PictureAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTransparentStatusBar()

        pictureAdapter = PictureAdapter(this)
        binding.rvPicture.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 5)
            adapter = pictureAdapter

            val itemDecoration = PictureItemDecoration(
                spanCount = 5,
                spacingX = 0,
                spacingY = 20
            )
            addItemDecoration(itemDecoration)
        }


        binding.tvAllPicture.setOnClickListener {

            viewModel.getPictureList()


        }


        lifecycleScope.launch {

            viewModel.pictureList.collect { list ->

                pictureAdapter.setData(list)

            }

        }



    }


}