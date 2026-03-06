package com.sansan.picture.model

import com.sansan.picture.repository.PictureRepository

data class Picture(
    val id: Long,
    val path: String,
    val name: String,
    val relativePath: String = "",
    val uri: String,
    val size: Long,
    val dateAdded: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val storageType: PictureRepository.StorageType
)