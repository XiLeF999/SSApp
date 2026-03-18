package com.sansan.picture.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sansan.picture.model.Picture
import com.sansan.picture.model.PictureFolder
import com.sansan.picture.repository.PictureRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val pictureRepository  = PictureRepository(application.applicationContext)

    private val _pictureList = MutableSharedFlow<List<Picture>>()
    val pictureList: SharedFlow<List<Picture>> = _pictureList

    //包含图片的文件夹
    private val _pictureFolderList = MutableSharedFlow<List<PictureFolder>>()
    val pictureFolderList: SharedFlow<List<PictureFolder>> = _pictureFolderList

    private val _deletePictureCount = MutableSharedFlow<Int>()
    val deletePictureCount: SharedFlow<Int> = _deletePictureCount

    private var folderId = ""
    private var storageType = PictureRepository.StorageType.ALL


    fun getPicturesInFolder(folderId: String = this.folderId) {

        this.folderId = folderId

        viewModelScope.launch {
            _pictureList.emit(pictureRepository.getPicturesInFolder(folderId))
        }
    }

    fun getPictureFolderList(storageType: PictureRepository.StorageType = this.storageType) {

        this.storageType = storageType

        viewModelScope.launch {
            _pictureFolderList.emit(pictureRepository.getPictureFolders(storageType))
        }
    }



    fun deletePictures(pictureList: List<Picture>) {
        viewModelScope.launch {
            _deletePictureCount.emit(pictureRepository.deletePictures(pictureList))
        }

    }




}