package com.example.data.download

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.model.MediaItem
import com.example.data.model.OutputFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.max

class DownloadEngine(
    private val context: Context,
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {
    private val downloadDao = database.downloadDao()
    private val activeJobs = mutableMapOf<String, Job>()
    private val downloadDir: File by lazy {
        val dir = File(context.filesDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val _tasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val tasks: StateFlow<Map<String, DownloadTask>> = _tasks.asStateFlow()

    init {
        // Load initial persisted downloads from Room
        scope.launch(Dispatchers.IO) {
            downloadDao.getAllDownloads().collect { entities ->
                val taskMap = entities.associate { it.id to it.toTask() }
                _tasks.update { current ->
                    // Merge active states with DB states
                    val merged = taskMap.toMutableMap()
                    current.forEach { (id, activeTask) ->
                        if (activeTask.status == DownloadStatus.DOWNLOADING || 
                            activeTask.status == DownloadStatus.CONVERTING) {
                            merged[id] = activeTask
                        }
                    }
                    merged
                }
            }
        }
    }

    fun enqueue(
        media: MediaItem,
        format: OutputFormat,
        quality: String
    ): String {
        val taskId = UUID.randomUUID().toString()
        val estimatedMb = if (format.isAudioOnly) {
            when {
                quality.contains("320") -> 8.5
                quality.contains("256") -> 6.8
                quality.contains("192") -> 5.2
                else -> 3.8
            }
        } else {
            when {
                quality.contains("1080") -> 52.0
                quality.contains("720") -> 28.0
                quality.contains("480") -> 16.0
                else -> 9.5
            }
        }
        val totalBytes = (estimatedMb * 1024 * 1024).toLong()

        val newTask = DownloadTask(
            id = taskId,
            mediaId = media.id,
            title = media.title,
            channel = media.channel,
            thumbnailUrl = media.thumbnailUrl,
            format = format,
            quality = quality,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = totalBytes,
            speedBytesPerSec = 0L,
            etaSeconds = 0L,
            status = DownloadStatus.QUEUED,
            createdAt = System.currentTimeMillis()
        )

        _tasks.update { it + (taskId to newTask) }

        scope.launch(Dispatchers.IO) {
            downloadDao.insertDownload(DownloadEntity.fromTask(newTask))
            processQueue()
        }

        return taskId
    }

    fun enqueueBatch(
        mediaList: List<MediaItem>,
        format: OutputFormat,
        quality: String
    ): List<String> {
        val taskIds = mutableListOf<String>()
        val newEntities = mutableListOf<DownloadEntity>()
        val newMap = mutableMapOf<String, DownloadTask>()

        mediaList.forEach { media ->
            val taskId = UUID.randomUUID().toString()
            taskIds.add(taskId)

            val estimatedMb = if (format.isAudioOnly) {
                when {
                    quality.contains("320") -> 8.5
                    quality.contains("256") -> 6.8
                    quality.contains("192") -> 5.2
                    else -> 3.8
                }
            } else {
                when {
                    quality.contains("1080") -> 52.0
                    quality.contains("720") -> 28.0
                    quality.contains("480") -> 16.0
                    else -> 9.5
                }
            }
            val totalBytes = (estimatedMb * 1024 * 1024).toLong()

            val newTask = DownloadTask(
                id = taskId,
                mediaId = media.id,
                title = media.title,
                channel = media.channel,
                thumbnailUrl = media.thumbnailUrl,
                format = format,
                quality = quality,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = totalBytes,
                status = DownloadStatus.QUEUED,
                createdAt = System.currentTimeMillis()
            )
            newEntities.add(DownloadEntity.fromTask(newTask))
            newMap[taskId] = newTask
        }

        _tasks.update { it + newMap }

        scope.launch(Dispatchers.IO) {
            downloadDao.insertDownloads(newEntities)
            processQueue()
        }

        return taskIds
    }

    private fun processQueue() {
        val currentRunning = _tasks.value.values.count { 
            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.CONVERTING 
        }
        val maxConcurrent = 2
        val availableSlots = maxConcurrent - currentRunning
        if (availableSlots <= 0) return

        val queuedTasks = _tasks.value.values
            .filter { it.status == DownloadStatus.QUEUED }
            .sortedBy { it.createdAt }
            .take(availableSlots)

        queuedTasks.forEach { task ->
            startDownloadJob(task.id)
        }
    }

    private fun startDownloadJob(taskId: String) {
        if (activeJobs.containsKey(taskId)) return

        val job = scope.launch(Dispatchers.IO) {
            val task = _tasks.value[taskId] ?: return@launch
            val safeTitle = task.title.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(40)
            val fileName = "${safeTitle}_${task.id.take(6)}.${task.format.extension}"
            val outputFile = File(downloadDir, fileName)

            // Update to DOWNLOADING
            updateTask(taskId) {
                it.copy(status = DownloadStatus.DOWNLOADING)
            }

            try {
                val totalBytes = max(task.totalBytes, 1024L * 1024L)
                var currentBytes = task.downloadedBytes

                // Real streaming simulator or direct HTTP stream
                val targetSpeedPerSec = when (task.format) {
                    OutputFormat.MP4 -> (4.5 * 1024 * 1024).toLong() // 4.5 MB/s
                    else -> (2.8 * 1024 * 1024).toLong() // 2.8 MB/s
                }

                val updateIntervalMs = 250L
                val bytesPerChunk = (targetSpeedPerSec * (updateIntervalMs / 1000.0)).toLong()

                // Generate valid dummy or synthesized media file with headers
                val fileOutput = FileOutputStream(outputFile, currentBytes > 0)
                
                // Write container header
                if (currentBytes == 0L) {
                    if (task.format.isAudioOnly) {
                        // ID3v2 container header mock
                        val id3Header = byteArrayOf(
                            0x49, 0x44, 0x33, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20
                        )
                        fileOutput.write(id3Header)
                        currentBytes += id3Header.size
                    } else {
                        // MP4 ftyp box header
                        val ftyp = byteArrayOf(
                            0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70,
                            0x6D, 0x70, 0x34, 0x32, 0x00, 0x00, 0x00, 0x00
                        )
                        fileOutput.write(ftyp)
                        currentBytes += ftyp.size
                    }
                }

                val buffer = ByteArray(16384) { (it % 128).toByte() }

                while (isActive && currentBytes < totalBytes) {
                    val remaining = totalBytes - currentBytes
                    val toWriteThisCycle = minOf(bytesPerChunk, remaining)
                    var written = 0L

                    while (written < toWriteThisCycle) {
                        val writeSize = minOf(buffer.size.toLong(), toWriteThisCycle - written).toInt()
                        fileOutput.write(buffer, 0, writeSize)
                        written += writeSize
                    }

                    currentBytes += written
                    val progress = (currentBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    val remainingBytes = totalBytes - currentBytes
                    val dynamicSpeed = (targetSpeedPerSec * (0.85 + (Math.random() * 0.3))).toLong()
                    val eta = if (dynamicSpeed > 0) remainingBytes / dynamicSpeed else 0L

                    updateTask(taskId) {
                        it.copy(
                            progress = progress,
                            downloadedBytes = currentBytes,
                            totalBytes = totalBytes,
                            speedBytesPerSec = dynamicSpeed,
                            etaSeconds = eta
                        )
                    }

                    delay(updateIntervalMs)
                }

                fileOutput.flush()
                fileOutput.close()

                if (!isActive) return@launch

                // Automatic conversion phase (MP3 / MP4 packaging & metadata tagging)
                updateTask(taskId) {
                    it.copy(
                        status = DownloadStatus.CONVERTING,
                        progress = 1.0f,
                        speedBytesPerSec = 0L,
                        etaSeconds = 0L
                    )
                }

                // Simulate audio extraction / video muxing time (1 - 1.5s)
                delay(1200L)

                // COMPLETED
                updateTask(taskId) {
                    it.copy(
                        status = DownloadStatus.COMPLETED,
                        progress = 1.0f,
                        downloadedBytes = totalBytes,
                        speedBytesPerSec = 0L,
                        etaSeconds = 0L,
                        localFilePath = outputFile.absolutePath,
                        completedAt = System.currentTimeMillis()
                    )
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    // Task cancelled or paused
                } else {
                    updateTask(taskId) {
                        it.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = e.message ?: "Download failed"
                        )
                    }
                }
            } finally {
                activeJobs.remove(taskId)
                processQueue()
            }
        }

        activeJobs[taskId] = job
    }

    fun pauseDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        updateTask(taskId) {
            it.copy(
                status = DownloadStatus.PAUSED,
                speedBytesPerSec = 0L,
                etaSeconds = 0L
            )
        }
        processQueue()
    }

    fun resumeDownload(taskId: String) {
        updateTask(taskId) {
            it.copy(status = DownloadStatus.QUEUED)
        }
        processQueue()
    }

    fun cancelDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        val task = _tasks.value[taskId]
        task?.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        _tasks.update { it - taskId }
        scope.launch(Dispatchers.IO) {
            downloadDao.deleteDownloadById(taskId)
            processQueue()
        }
    }

    fun retryDownload(taskId: String) {
        updateTask(taskId) {
            it.copy(
                status = DownloadStatus.QUEUED,
                progress = 0f,
                downloadedBytes = 0L,
                errorMessage = null
            )
        }
        processQueue()
    }

    fun deleteCompleted(taskId: String) {
        val task = _tasks.value[taskId]
        task?.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        _tasks.update { it - taskId }
        scope.launch(Dispatchers.IO) {
            downloadDao.deleteDownloadById(taskId)
        }
    }

    fun clearAllCompleted() {
        val completed = _tasks.value.values.filter { it.status == DownloadStatus.COMPLETED }
        completed.forEach { task ->
            task.localFilePath?.let { File(it).delete() }
        }
        _tasks.update { current ->
            current.filterValues { it.status != DownloadStatus.COMPLETED }
        }
        scope.launch(Dispatchers.IO) {
            downloadDao.clearCompleted()
        }
    }

    fun pauseAll() {
        val downloading = _tasks.value.values.filter { 
            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED 
        }
        downloading.forEach { pauseDownload(it.id) }
    }

    fun resumeAll() {
        val paused = _tasks.value.values.filter { it.status == DownloadStatus.PAUSED }
        paused.forEach {
            updateTask(it.id) { task -> task.copy(status = DownloadStatus.QUEUED) }
        }
        processQueue()
    }

    private fun updateTask(taskId: String, block: (DownloadTask) -> DownloadTask) {
        _tasks.update { current ->
            val existing = current[taskId] ?: return@update current
            val updated = block(existing)
            scope.launch(Dispatchers.IO) {
                downloadDao.updateDownload(DownloadEntity.fromTask(updated))
            }
            current + (taskId to updated)
        }
    }
}
