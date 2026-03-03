package com.sansan.picture.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sansan.picture.databinding.ActivityMainBinding
import com.sansan.picture.utils.setTransparentStatusBar
import com.sansan.picture.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.getValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTransparentStatusBar()


        binding.tvAllPicture.setOnClickListener {

            viewModel.getPictureList()


        }


        lifecycleScope.launch {

            viewModel.pictureList.collect { list ->

                list.forEach {

                    Log.e("wwww", it.name)

                }

            }

        }



    }


}