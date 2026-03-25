package com.sansan.video.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    // 当前播放进度
    private val _currentProgress  = MutableStateFlow<Long>(0)
    val currentProgress : StateFlow<Long> = _currentProgress
    private var progressJob: Job? = null

    // 显示隐藏
    private val _isShowController  = MutableSharedFlow<Boolean>()
    val isShowController : SharedFlow<Boolean> = _isShowController
    private var showControllerJob: Job? = null

    fun startProgressUpdates(player: ExoPlayer?) {
        progressJob?.cancel() // 取消之前的任务
        progressJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _currentProgress.value = player?.currentPosition?:0
            }
        }
    }


    fun showVideoControl(){
        showControllerJob?.cancel() // 取消之前的任务
        showControllerJob = viewModelScope.launch {
            _isShowController.emit(true)
            delay(2500)
            _isShowController.emit(false)
        }
    }


}