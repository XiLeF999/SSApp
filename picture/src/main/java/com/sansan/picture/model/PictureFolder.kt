package com.sansan.picture.model

import com.sansan.picture.repository.PictureRepository

data class PictureFolder(
    val id: String,
    val name: String,
    val path: String,
    val coverPath: String,
    var count: Int,
    val storageType: PictureRepository.StorageType
)