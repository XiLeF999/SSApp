package com.sansan.picture.model

data class PictureModel(
    val id: Long,
    val path: String,
    val name: String,
    val uri: String,
    val size: Long,
    val dateAdded: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
)