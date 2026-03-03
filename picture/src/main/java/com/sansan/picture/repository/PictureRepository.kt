package com.sansan.picture.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import com.sansan.picture.model.PictureModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PictureRepository(private val context: Context) {

    suspend fun getAllPictures(): List<PictureModel> = withContext(Dispatchers.IO) {
        val pictures = mutableListOf<PictureModel>()
        val contentResolver: ContentResolver = context.contentResolver

        // 查询图片的列
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,          // 文件路径（已废弃但仍有需要）
            MediaStore.Images.Media.RELATIVE_PATH, // 相对路径（Android 10+）
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE
        )

        // 排序方式：按添加时间倒序
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,  // 筛选条件，可以添加如：${MediaStore.Images.Media.MIME_TYPE} LIKE 'image/%'
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val path = it.getString(pathColumn) ?: ""
                val size = it.getLong(sizeColumn)
                val dateAdded = it.getLong(dateColumn)
                val width = it.getInt(widthColumn)
                val height = it.getInt(heightColumn)
                val mimeType = it.getString(mimeColumn) ?: "image/*"

                // 构建 URI
                val uri = "${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}/$id"

                pictures.add(
                    PictureModel(
                        id = id,
                        name = name,
                        path = path,
                        uri = uri,
                        size = size,
                        dateAdded = dateAdded,
                        width = width,
                        height = height,
                        mimeType = mimeType
                    )
                )
            }
        }

        return@withContext pictures
    }

    // 获取特定相册的图片
    suspend fun getPicturesByAlbum(albumName: String): List<PictureModel> {
        // 实现类似逻辑，添加筛选条件
        return emptyList()
    }
}