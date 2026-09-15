package com.sansan.picture.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sansan.picture.model.Picture
import com.sansan.picture.repository.PictureRepository
import kotlinx.coroutines.launch

class PictureViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PictureRepository(application)

    fun deletePicture(picture: Picture, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(repository.deletePicture(picture.path))
        }
    }
}