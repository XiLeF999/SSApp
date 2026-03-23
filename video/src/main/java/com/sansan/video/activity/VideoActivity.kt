package com.sansan.video.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.sansan.video.databinding.ActivityVideoBinding
import com.sansan.video.model.Video
import com.sansan.video.utils.setFullScreen
import com.sansan.video.viewmodel.VideoViewModel

class VideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoBinding

    private val viewModel: VideoViewModel by viewModels()

    private lateinit var currentVideo: Video

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFullScreen()

        currentVideo = intent.getSerializableExtra("current_video") as Video

        player = ExoPlayer.Builder(this).build()

        binding.playerView.player = player

        val mediaItem = MediaItem.fromUri(currentVideo.path)
        player?.setMediaItem(mediaItem)

        player?.prepare()
        player?.playWhenReady = true // 准备完成后自动播放

    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release() // 必须释放资源
        player = null
    }

}