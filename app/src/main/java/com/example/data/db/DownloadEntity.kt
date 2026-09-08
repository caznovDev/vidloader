package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.model.OutputFormat

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val mediaId: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String,
    val format: String,
    val quality: String,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
    val status: String,
    val localFilePath: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val completedAt: Long?
) {
    fun toTask(): DownloadTask {
        val outputFormat = try {
            OutputFormat.valueOf(format)
        } catch (e: Exception) {
            OutputFormat.MP3
        }
        val downloadStatus = try {
            DownloadStatus.valueOf(status)
        } catch (e: Exception) {
            DownloadStatus.FAILED
        }
        return DownloadTask(
            id = id,
            mediaId = mediaId,
            title = title,
            channel = channel,
            thumbnailUrl = thumbnailUrl,
            format = outputFormat,
            quality = quality,
            progress = progress,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            speedBytesPerSec = speedBytesPerSec,
            etaSeconds = etaSeconds,
            status = downloadStatus,
            localFilePath = localFilePath,
            errorMessage = errorMessage,
            createdAt = createdAt,
            completedAt = completedAt
        )
    }

    companion object {
        fun fromTask(task: DownloadTask): DownloadEntity {
            return DownloadEntity(
                id = task.id,
                mediaId = task.mediaId,
                title = task.title,
                channel = task.channel,
                thumbnailUrl = task.thumbnailUrl,
                format = task.format.name,
                quality = task.quality,
                progress = task.progress,
                downloadedBytes = task.downloadedBytes,
                totalBytes = task.totalBytes,
                speedBytesPerSec = task.speedBytesPerSec,
                etaSeconds = task.etaSeconds,
                status = task.status.name,
                localFilePath = task.localFilePath,
                errorMessage = task.errorMessage,
                createdAt = task.createdAt,
                completedAt = task.completedAt
            )
        }
    }
}
