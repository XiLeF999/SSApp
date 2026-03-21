package com.sansan.video.model

import com.sansan.video.repository.VideoRepository
import java.io.Serializable

data class Picture(
    val id: Long,
    val path: String,
    val name: String,
    val relativePath: String = "",
    val uri: String,
    val size: Long,
    val dateTaken: Long,  //照片的拍摄时间（Unix 时间戳，毫秒）
    val width: Int,
    val height: Int,
    val mimeType: String,
    val storageType: VideoRepository.StorageType,
    var isSelect : Boolean = false
): Serializable