package com.sansan.picture.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sansan.picture.model.Picture
import com.sansan.picture.model.PictureFolder
import com.sansan.picture.repository.PictureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val pictureRepository  = PictureRepository(application.applicationContext)

    private val _pictureList = MutableStateFlow<List<Picture>>(emptyList())
    val pictureList: StateFlow<List<Picture>> = _pictureList

    //包含图片的文件夹
    private val _pictureFolderList = MutableStateFlow<List<PictureFolder>>(emptyList())
    val pictureFolderList: StateFlow<List<PictureFolder>> = _pictureFolderList


    fun getPicturesInFolder(folderId: String) {
        viewModelScope.launch {

            _pictureList.value = emptyList()   //先清除，免得数据闪烁

            _pictureList.value = pictureRepository.getPicturesInFolder(folderId)
        }
    }

    fun getPictureFolderList(storageType: PictureRepository.StorageType) {
        viewModelScope.launch {

            _pictureFolderList.value = pictureRepository.getPictureFolders(storageType)
        }
    }



}