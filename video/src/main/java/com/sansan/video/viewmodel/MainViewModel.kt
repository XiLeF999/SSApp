package com.sansan.video.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sansan.video.model.Picture
import com.sansan.video.model.PictureFolder
import com.sansan.video.model.Video
import com.sansan.video.model.VideoFolder
import com.sansan.video.repository.VideoRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
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


    fun getVideosInFolder(folderId: String = this.folderId) {

        this.folderId = folderId

        viewModelScope.launch {
            _videoList.emit(videoRepository.getVideosInFolder(folderId))
        }
    }

    fun getVideoFolderList(storageType: VideoRepository.StorageType = this.storageType) {

        this.storageType = storageType

        viewModelScope.launch {
            _videoFolderList.emit(videoRepository.getVideoFolders(storageType))
        }
    }



    fun deleteVideos(videoList: List<Video>) {
        viewModelScope.launch {
            _deleteVideoCount.emit(videoRepository.deleteVideos(videoList))
        }

    }




}