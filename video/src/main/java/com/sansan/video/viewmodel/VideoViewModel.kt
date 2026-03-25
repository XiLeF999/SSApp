package com.sansan.video.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    // 当前播放进度
    private val _currentProgress  = MutableSharedFlow<Long>()
    val currentProgress : SharedFlow<Long> = _currentProgress

    private var progressJob: Job? = null

    fun startProgressUpdates(player: ExoPlayer?) {
        progressJob?.cancel() // 取消之前的任务
        progressJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _currentProgress.emit(player?.currentPosition?:0)
            }
        }
    }

}