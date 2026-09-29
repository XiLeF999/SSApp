package com.sansan.video.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sansan.video.model.Video
import com.sansan.video.model.VideoFolder
import com.sansan.video.repository.VideoRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val videoRepository  = VideoRepository(application.applicationContext)

    private val _videoList = MutableSharedFlow<List<Video>>()
    val videoList: SharedFlow<List<Video>> = _videoList

    //包含图片的文件夹
    private val _videoFolderList = MutableSharedFlow<List<VideoFolder>>()
    val videoFolderList: SharedFlow<List<VideoFolder>> = _videoFolderList

    private val _deleteVideoCount = MutableSharedFlow<Int>()
    val deleteVideoCount: SharedFlow<Int> = _deleteVideoCount

    private var folderId = ""
    private var storageType = VideoRepository.StorageType.ALL
    private var folderLoadJob: Job? = null
    private var videoLoadJob: Job? = null
    private var folderScanJob: Deferred<List<VideoFolder>>? = null
    private val folderVideoCache = mutableMapOf<String, List<Video>>()


    fun getVideosInFolder(folderId: String = this.folderId) {
        this.folderId = folderId

        videoLoadJob?.cancel()
        videoLoadJob = viewModelScope.launch {
            val videos = folderVideoCache[folderId] ?: videoRepository.getVideosInFolder(folderId).also {
                folderVideoCache[folderId] = it
            }
            _videoList.emit(videos)
        }
    }

    fun getVideoFolderList(storageType: VideoRepository.StorageType = this.storageType) {
        this.storageType = storageType

        folderLoadJob?.cancel()
        folderLoadJob = viewModelScope.launch {
            // Scan MediaStore once after entering the app, then filter the cached result.
            val allFolders = folderScanJob?.await() ?: viewModelScope.async {
                videoRepository.getVideoFolders(VideoRepository.StorageType.ALL)
            }.also { folderScanJob = it }.await()
            _videoFolderList.emit(
                if (storageType == VideoRepository.StorageType.ALL) allFolders
                else allFolders.filter { it.storageType == storageType }
            )
        }
    }


    fun deleteVideos(videoList: List<Video>) {
        viewModelScope.launch {
            _deleteVideoCount.emit(videoRepository.deleteVideos(videoList))
        }

    }




}