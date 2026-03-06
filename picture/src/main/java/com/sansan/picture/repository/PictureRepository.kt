package com.sansan.picture.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import com.sansan.picture.model.Picture
import com.sansan.picture.model.PictureFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PictureRepository(private val context: Context) {

    enum class StorageType {
        ALL,        // 所有图片
        INTERNAL,   // 内部存储
        SD_CARD,    // SD卡
        USB         // USB设备
    }

    // 获取所有包含图片的文件夹
    suspend fun getPictureFolders(storageType: StorageType = StorageType.ALL): List<PictureFolder> = withContext(Dispatchers.IO) {
        val folderMap = HashMap<String, PictureFolder>()
        val folders = mutableListOf<PictureFolder>()

        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID
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
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val bucketNameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val bucketIdColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val path = it.getString(dataColumn) ?: ""
                val bucketName = it.getString(bucketNameColumn) ?: "未知文件夹"
                val bucketId = it.getString(bucketIdColumn) ?: id.toString()

                val detectedType = detectStorageType(path)

                // 根据 storageType 参数过滤
                if (storageType == StorageType.ALL || storageType == detectedType) {
                    val folder = folderMap[bucketId]

                    if (folder == null) {
                        // 创建文件夹路径（去掉文件名）
                        val folderPath = if (path.isNotEmpty() && path.contains("/")) {
                            path.substring(0, path.lastIndexOf("/"))
                        } else {
                            ""
                        }

                        val newFolder = PictureFolder(
                            id = bucketId,
                            name = bucketName,
                            path = folderPath,
                            coverPath = path,
                            count = 1,
                            storageType = detectedType
                        )
                        folderMap[bucketId] = newFolder
                        folders.add(newFolder)
                    } else {
                        folder.count++
                    }
                }
            }
        }

        // 按图片数量排序
        return@withContext folders.sortedByDescending { it.count }
    }

    // 获取指定文件夹下的所有图片
    suspend fun getPicturesInFolder(folderId: String): List<Picture> = withContext(Dispatchers.IO) {
        val pictures = mutableListOf<Picture>()
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
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_ID
        )

        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(folderId)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
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

                val uri = "${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}/$id"

                pictures.add(
                    Picture(
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

        return@withContext pictures
    }

    // 获取指定存储类型的所有文件夹
    suspend fun getImageFoldersByType(storageType: StorageType): List<PictureFolder> {
        val allFolders = getPictureFolders(StorageType.ALL)
        return allFolders.filter { it.storageType == storageType }
    }

    // 按图片数量筛选文件夹
    suspend fun getImageFoldersWithMinCount(minCount: Int = 1): List<PictureFolder> {
        val allFolders = getPictureFolders(StorageType.ALL)
        return allFolders.filter { it.count >= minCount }
    }

    // 获取文件夹统计信息
    suspend fun getFolderStatistics(): Map<StorageType, Int> = withContext(Dispatchers.IO) {
        val folders = getPictureFolders(StorageType.ALL)
        val statistics = mutableMapOf<StorageType, Int>()

        StorageType.values().forEach { type ->
            if (type != StorageType.ALL) {
                val count = folders.count { it.storageType == type }
                statistics[type] = count
            }
        }

        return@withContext statistics
    }

    // 保留你原有的方法
    suspend fun getPictures(storageType: StorageType = StorageType.ALL): List<Picture> = withContext(Dispatchers.IO) {
        val pictures = mutableListOf<Picture>()
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

                if (storageType == StorageType.ALL || storageType == detectedType) {
                    val uri = "${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}/$id"

                    pictures.add(
                        Picture(
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

        // 2. 检测USB存储
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
        if ((lowerPath.contains("/storage/sdcard") &&
                    !lowerPath.contains("/storage/sdcard/0")) ||
            lowerPath.contains("/extsd") ||
            lowerPath.contains("/external_sd") ||
            lowerPath.contains("/ext_sd") ||
            lowerPath.contains("/sdcard1") ||
            lowerPath.contains("/sdcard2")) {
            return StorageType.SD_CARD
        }

        return StorageType.INTERNAL
    }

    suspend fun getUSBPhotos(): List<Picture> = getPictures(StorageType.USB)

    suspend fun getInternalPhotos(): List<Picture> = getPictures(StorageType.INTERNAL)

    suspend fun getSDCardPhotos(): List<Picture> = getPictures(StorageType.SD_CARD)
}