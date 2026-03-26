package com.sansan.video.repository

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.sansan.video.model.Video
import com.sansan.video.model.VideoFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {

    enum class StorageType {
        ALL,        // 所有视频
        INTERNAL,   // 内部存储
        SD_CARD,    // SD卡
        USB         // USB设备
    }


    // 获取所有视频
    suspend fun getVideos(storageType: StorageType = StorageType.ALL): List<Video> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<Video>()
        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"

        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val relativePathColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val mimeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val path = it.getString(pathColumn) ?: ""
                val relativePath = it.getString(relativePathColumn) ?: ""
                val size = it.getLong(sizeColumn)
                val dateTaken = it.getLong(dateColumn)
                val duration = it.getLong(durationColumn)
                val width = it.getInt(widthColumn)
                val height = it.getInt(heightColumn)
                val mimeType = it.getString(mimeColumn) ?: "video/*"

                val detectedType = detectStorageType(path)

                if (storageType == StorageType.ALL || storageType == detectedType) {
                    val uri = "${MediaStore.Video.Media.EXTERNAL_CONTENT_URI}/$id"

                    videos.add(
                        Video(
                            id = id,
                            name = name,
                            path = path,
                            relativePath = relativePath,
                            uri = uri,
                            size = size,
                            dateTaken = dateTaken,
                            duration = duration,
                            width = width,
                            height = height,
                            mimeType = mimeType,
                            storageType = detectedType
                        )
                    )
                }
            }
        }

        return@withContext videos
    }

    // 获取所有包含视频的文件夹
    suspend fun getVideoFolders(storageType: StorageType = StorageType.ALL): List<VideoFolder> = withContext(Dispatchers.IO) {
        val folderMap = HashMap<String, VideoFolder>()
        val folders = mutableListOf<VideoFolder>()

        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"

        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val bucketNameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val bucketIdColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)

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

                        val newFolder = VideoFolder(
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

        // 按视频数量排序
        return@withContext folders.sortedByDescending { it.count }
    }

    // 获取指定文件夹下的所有视频
    suspend fun getVideosInFolder(folderId: String): List<Video> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<Video>()
        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_ID
        )

        val selection = "${MediaStore.Video.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(folderId)
        val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"

        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val relativePathColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val mimeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val path = it.getString(pathColumn) ?: ""
                val relativePath = it.getString(relativePathColumn) ?: ""
                val size = it.getLong(sizeColumn)
                val dateTaken = it.getLong(dateColumn)
                var duration = it.getLong(durationColumn)
                if(duration <= 0){
                    duration = getVideoDurationWithRetriever(path)
                }
                val width = it.getInt(widthColumn)
                val height = it.getInt(heightColumn)
                val mimeType = it.getString(mimeColumn) ?: "video/*"

                val detectedType = detectStorageType(path)
                val uri = "${MediaStore.Video.Media.EXTERNAL_CONTENT_URI}/$id"

                videos.add(
                    Video(
                        id = id,
                        name = name,
                        path = path,
                        relativePath = relativePath,
                        uri = uri,
                        size = size,
                        dateTaken = dateTaken,
                        duration = duration,
                        width = width,
                        height = height,
                        mimeType = mimeType,
                        storageType = detectedType
                    )
                )
            }
        }

        return@withContext videos
    }

    // 删除单个视频
    suspend fun deleteVideo(videoPath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val selection = "${MediaStore.Video.Media.DATA} = ?"
            val selectionArgs = arrayOf(videoPath)

            val rowsDeleted = context.contentResolver.delete(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                selection,
                selectionArgs
            )

            rowsDeleted > 0
        } catch (e: Exception) {
            false
        }
    }

    // 批量删除视频
    suspend fun deleteVideos(videoList: List<Video>): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            var totalDeleted = 0

            videoList.forEach { video ->
                val selection = "${MediaStore.Video.Media.DATA} = ?"
                val selectionArgs = arrayOf(video.path)

                val rowsDeleted = context.contentResolver.delete(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    selection,
                    selectionArgs
                )

                if (rowsDeleted > 0) {
                    totalDeleted++
                }
            }

            totalDeleted
        } catch (e: Exception) {
            0
        }
    }


    suspend fun getVideoDurationWithRetriever(filePath: String): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        return@withContext try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }



    // 获取指定存储类型的所有视频文件夹
    suspend fun getVideoFoldersByType(storageType: StorageType): List<VideoFolder> {
        val allFolders = getVideoFolders(StorageType.ALL)
        return allFolders.filter { it.storageType == storageType }
    }

    // 获取文件夹统计信息
    suspend fun getFolderStatistics(): Map<StorageType, Int> = withContext(Dispatchers.IO) {
        val folders = getVideoFolders(StorageType.ALL)
        val statistics = mutableMapOf<StorageType, Int>()

        StorageType.values().forEach { type ->
            if (type != StorageType.ALL) {
                val count = folders.count { it.storageType == type }
                statistics[type] = count
            }
        }

        return@withContext statistics
    }

    // 按视频数量筛选文件夹
    suspend fun getVideoFoldersWithMinCount(minCount: Int = 1): List<VideoFolder> {
        val allFolders = getVideoFolders(StorageType.ALL)
        return allFolders.filter { it.count >= minCount }
    }

    // 获取USB设备中的视频
    suspend fun getUSBVideos(): List<Video> = getVideos(StorageType.USB)

    // 获取内部存储中的视频
    suspend fun getInternalVideos(): List<Video> = getVideos(StorageType.INTERNAL)

    // 获取SD卡中的视频
    suspend fun getSDCardVideos(): List<Video> = getVideos(StorageType.SD_CARD)

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
}