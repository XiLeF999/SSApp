package com.sansan.picture.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sansan.picture.model.PictureModel
import com.sansan.picture.repository.PictureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _pictureList = MutableStateFlow<List<PictureModel>>(emptyList())
    val pictureList: StateFlow<List<PictureModel>> = _pictureList

    private val pictureRepository  = PictureRepository(application.applicationContext)


    fun getPictureList() {

    }

    fun updatePictureList() {

    }



}