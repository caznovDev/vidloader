package com.example.data.model

enum class MediaType {
    VIDEO,
    AUDIO
}

enum class OutputFormat(val extension: String, val displayName: String, val isAudioOnly: Boolean) {
    MP3("mp3", "MP3 (Audio)", true),
    MP4("mp4", "MP4 (Video)", false),
    M4A("m4a", "M4A (AAC Audio)", true),
    FLAC("flac", "FLAC (Lossless)", true)
}

data class QualityOption(
    val label: String,
    val resolutionOrBitrate: String,
    val estimatedSizeMb: Double,
    val isRecommended: Boolean = false
)

data class MediaItem(
    val id: String,
    val title: String,
    val channel: String,
    val duration: String,
    val durationSeconds: Long = 210,
    val thumbnailUrl: String,
    val views: String = "1.4M views",
    val publishedTime: String = "3 days ago",
    val mediaType: MediaType = MediaType.VIDEO,
    val sourceUrl: String = "https://www.youtube.com/watch?v=$id",
    val availableAudioQualities: List<QualityOption> = listOf(
        QualityOption("High Quality", "320 kbps", 8.4, isRecommended = true),
        QualityOption("Standard", "256 kbps", 6.8),
        QualityOption("Medium", "192 kbps", 5.2),
        QualityOption("Compact", "128 kbps", 3.6)
    ),
    val availableVideoQualities: List<QualityOption> = listOf(
        QualityOption("Full HD 1080p", "1080p 60fps", 48.5, isRecommended = true),
        QualityOption("HD 720p", "720p 30fps", 26.2),
        QualityOption("SD 480p", "480p", 15.1),
        QualityOption("Low 360p", "360p", 9.4)
    )
)

data class Playlist(
    val id: String,
    val title: String,
    val creator: String,
    val thumbnailUrl: String,
    val description: String,
    val trackCount: Int,
    val tracks: List<MediaItem>
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    CONVERTING,
    COMPLETED,
    PAUSED,
    FAILED
}

data class DownloadTask(
    val id: String,
    val mediaId: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String,
    val format: OutputFormat,
    val quality: String,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val localFilePath: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
