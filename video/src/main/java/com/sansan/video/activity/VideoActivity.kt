package com.sansan.video.activity

import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.sansan.video.R
import com.sansan.video.databinding.ActivityVideoBinding
import com.sansan.video.model.Video
import com.sansan.video.utils.hide
import com.sansan.video.utils.setFullScreen
import com.sansan.video.utils.show
import com.sansan.video.utils.toChineseDate
import com.sansan.video.utils.toChineseTime
import com.sansan.video.utils.toKB2Decimal
import com.sansan.video.utils.truncateMiddle
import com.sansan.video.utils.wrapEvery
import com.sansan.video.viewmodel.VideoViewModel
import com.sansan.video.widget.VideoFrameLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
        player?.repeatMode = Player.REPEAT_MODE_ONE  //自动重播

        player?.addListener(playerListener)

        binding.playerView.player = player

        val mediaItem = MediaItem.fromUri(currentVideo.path)
        player?.setMediaItem(mediaItem)

        player?.prepare()
        player?.playWhenReady = true // 准备完成后自动播放


        binding.ivPlayPause.setOnClickListener {
            player?.let { exoPlayer ->
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    binding.ivPlayPause.setImageResource(R.mipmap.img_pic_ic_pause)
                } else {
                    exoPlayer.play()
                    binding.ivPlayPause.setImageResource(R.mipmap.img_pic_ic_play)
                }
            }
        }

        binding.ivClose.setOnClickListener {
            finish()
        }


        binding.flVideo.setOnVideoFrameLayoutClickListener(object :
            VideoFrameLayout.OnVideoFrameLayoutClickListener {
            override fun onVideoFrameLayoutClick() {
                viewModel.showVideoControl()
            }
        })


        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                p0: SeekBar?,
                p1: Int,
                p2: Boolean
            ) {
                if (p2) {
                    player?.seekTo(p1.toLong())
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })


        lifecycleScope.launch {
            //快速变化的用户操作，用 collectLatest，只显示最后一次结果。用了这个就不用手动cancel协程Job了
            viewModel.currentProgress.collectLatest { currentProgress ->

                binding.tvCurrentTime.text = currentProgress?.let {
                    val minutes = it / 1000 / 60
                    val seconds = it / 1000 % 60
                    String.format("%02d:%02d", minutes, seconds)
                }

                binding.seekBar.progress = currentProgress?.toInt() ?: 0

            }
        }


        lifecycleScope.launch {
            //快速变化的用户操作，用 collectLatest，只显示最后一次结果。用了这个就不用手动cancel协程Job了
            viewModel.isShowController.collectLatest { show ->
                if (show) {
                    binding.flVideoController.show()
                } else {
                    binding.flVideoController.hide()
                }
            }
        }


        binding.tvVideoDate.text = currentVideo.dateTaken.toChineseDate()
        binding.tvVideoTime.text = currentVideo.dateTaken.toChineseTime()
        binding.tvVideoName.text = currentVideo.name.truncateMiddle()
        binding.tvVideoResolution.text = currentVideo.width.toString() + "*" + currentVideo.height.toString()
        binding.tvVideoSize.text = currentVideo.size.toKB2Decimal()
        binding.tvVideoPath.text = ("路径:" + currentVideo.path).wrapEvery()


    }


    var playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    // 播放结束时的处理
                    Log.e("wwww", "视频播放结束")
                    binding.ivPlayPause.setImageResource(R.mipmap.img_pic_ic_pause)
                    // 这里可以添加其他逻辑，比如显示重播按钮、自动播放下一个视频等
                }

                Player.STATE_BUFFERING -> {
                    Log.e("wwww", "正在缓冲")
                }

                Player.STATE_READY -> {
                    Log.e("wwww", "准备就绪")

                    var duration = player?.duration ?: 0
                    binding.tvTotalTime.text = duration?.let {
                        val minutes = it / 1000 / 60
                        val seconds = it / 1000 % 60
                        String.format("%02d:%02d", minutes, seconds)
                    }

                    viewModel.showVideoControl()
                    viewModel.startProgressUpdates(player)

                    binding.seekBar.max = duration.toInt()

                }

                Player.STATE_IDLE -> {
                    Log.e("wwww", "空闲状态")
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            // 播放出错时的处理
            Log.e("wwww", "播放错误: ${error.message}")
            Toast.makeText(this@VideoActivity, "不支持播放本视频", Toast.LENGTH_SHORT).show()
            binding.ivPlayPause.setImageResource(R.mipmap.img_pic_ic_pause)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // 播放状态变化
            if (isPlaying) {
                binding.ivPlayPause.setImageResource(R.mipmap.img_pic_ic_play)
            } else {
                binding.ivPlayPause.setImageResource(R.mipmap.img_pic_ic_pause)
            }
        }
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