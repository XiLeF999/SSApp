package com.sansan.video.model

import com.sansan.video.repository.VideoRepository

data class VideoFolder(
    val id: String,
    val name: String,
    val path: String,
    val coverPath: String,
    var count: Int,
    val storageType: VideoRepository.StorageType
)