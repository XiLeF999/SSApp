package com.sansan.video.model

import com.sansan.video.repository.VideoRepository

data class Video(
    val id: Long,
    val name: String,
    val path: String,
    val relativePath: String,
    val uri: String,
    val size: Long,
    val dateTaken: Long,
    val duration: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val storageType: VideoRepository.StorageType,
    var isSelect : Boolean = false
)