package com.sansan.video.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    // 当前播放进度
    private val _currentProgress  = MutableSharedFlow<Long>()
    val currentProgress : SharedFlow<Long> = _currentProgress

    fun startProgressUpdates(player: ExoPlayer?) {
        viewModelScope.launch {
            while (true) {
                delay(1000) // 每秒更新一次
                _currentProgress.emit(player?.currentPosition?:0)
            }
        }
    }

}