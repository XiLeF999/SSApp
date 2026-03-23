package com.sansan.video.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sansan.video.databinding.ActivityMainBinding
import com.sansan.video.databinding.ActivityVideoBinding
import com.sansan.video.model.Video
import com.sansan.video.utils.setFullScreen
import com.sansan.video.viewmodel.VideoViewModel

class VideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoBinding

    private val viewModel: VideoViewModel by viewModels()

    private lateinit var currentVideo: Video

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFullScreen()

        currentVideo = intent.getSerializableExtra("current_video") as Video

    }

}