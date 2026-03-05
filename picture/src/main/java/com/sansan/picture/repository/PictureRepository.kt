package com.sansan.picture.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.sansan.picture.model.PictureModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PictureRepository(private val context: Context) {

    enum class StorageType {
        ALL,        // 所有图片
        INTERNAL,   // 内部存储
        SD_CARD,    // SD卡
        USB         // USB设备
    }

    suspend fun getPictures(storageType: StorageType = StorageType.ALL): List<PictureModel> = withContext(Dispatchers.IO) {
        val pictures = mutableListOf<PictureModel>()
        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val relativePathColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val path = it.getString(pathColumn) ?: ""
                val relativePath = it.getString(relativePathColumn) ?: ""
                val size = it.getLong(sizeColumn)
                val dateAdded = it.getLong(dateColumn)
                val width = it.getInt(widthColumn)
                val height = it.getInt(heightColumn)
                val mimeType = it.getString(mimeColumn) ?: "image/*"

                val detectedType = detectStorageType(path)

                // 根据 storageType 参数过滤
                if (storageType == StorageType.ALL || storageType == detectedType) {
                    val uri = "${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}/$id"

                    pictures.add(
                        PictureModel(
                            id = id,
                            name = name,
                            path = path,
                            relativePath = relativePath,
                            uri = uri,
                            size = size,
                            dateAdded = dateAdded,
                            width = width,
                            height = height,
                            mimeType = mimeType,
                            storageType = detectedType
                        )
                    )
                }
            }
        }

        return@withContext pictures
    }

    private fun detectStorageType(path: String): StorageType {
        if (path.isBlank()) return StorageType.INTERNAL

        val lowerPath = path.lowercase()

        // 1. 检测内部存储
        if (lowerPath.contains("/storage/emulated") ||
            lowerPath.contains("/data/data") ||
            lowerPath.contains("/data/user")) {
            return StorageType.INTERNAL
        }

        // 2. 检测USB存储 - 通过路径模式判断
        // USB路径格式通常为: /storage/xxxx-xxxx/ 或 /mnt/media_rw/xxxx-xxxx/
        val usbPattern = Regex("""^/storage/[0-9a-f]{4}-[0-9a-f]{4}/.*""")
        val mntUsbPattern = Regex("""^/mnt/media_rw/[0-9a-f]{4}-[0-9a-f]{4}/.*""")
        val usbPattern2 = Regex("""^/mnt/usbotg.*""")
        val usbPattern3 = Regex("""^/storage/usbotg.*""")

        if (usbPattern.matches(lowerPath) ||
            mntUsbPattern.matches(lowerPath) ||
            usbPattern2.matches(lowerPath) ||
            usbPattern3.matches(lowerPath) ||
            lowerPath.contains("/usb") ||
            lowerPath.contains("/usbstorage") ||
            lowerPath.contains("/media_rw")) {
            return StorageType.USB
        }

        // 3. 检测SD卡
        // SD卡路径通常为: /storage/sdcard1/, /storage/extSdCard/, /storage/ext_sd/ 等
        if ((lowerPath.contains("/storage/sdcard") &&
                    !lowerPath.contains("/storage/sdcard/0")) || // 排除内部存储
            lowerPath.contains("/extsd") ||
            lowerPath.contains("/external_sd") ||
            lowerPath.contains("/ext_sd") ||
            lowerPath.contains("/sdcard1") ||
            lowerPath.contains("/sdcard2")) {
            return StorageType.SD_CARD
        }

        // 4. 默认内部存储
        return StorageType.INTERNAL
    }

    suspend fun getUSBPhotos(): List<PictureModel> = getPictures(StorageType.USB)

    suspend fun getInternalPhotos(): List<PictureModel> = getPictures(StorageType.INTERNAL)

    suspend fun getSDCardPhotos(): List<PictureModel> = getPictures(StorageType.SD_CARD)
}