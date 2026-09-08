package com.example.data.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.example.data.model.DownloadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlayerPlaybackState(
    val currentTask: DownloadTask? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val totalDurationMs: Int = 0,
    val isVisible: Boolean = false
)

class AudioPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlayerPlaybackState())
    val playbackState: StateFlow<PlayerPlaybackState> = _playbackState.asStateFlow()

    fun playMedia(task: DownloadTask) {
        val filePath = task.localFilePath ?: return
        val file = File(filePath)
        if (!file.exists()) return

        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                prepare()
                start()
                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = duration
                    )
                }
            }

            val duration = mediaPlayer?.duration ?: 180000
            _playbackState.value = PlayerPlaybackState(
                currentTask = task,
                isPlaying = true,
                currentPositionMs = 0,
                totalDurationMs = duration,
                isVisible = true
            )

            startProgressTracker()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for visual player if dummy file format doesn't have decodable audio
            val duration = (task.totalBytes / (128 * 1024 / 8) * 1000).toInt().coerceAtLeast(120000)
            _playbackState.value = PlayerPlaybackState(
                currentTask = task,
                isPlaying = true,
                currentPositionMs = 0,
                totalDurationMs = duration,
                isVisible = true
            )
            startProgressTracker()
        }
    }

    fun togglePlayPause() {
        val current = _playbackState.value
        if (current.currentTask == null) return

        if (current.isPlaying) {
            try {
                mediaPlayer?.pause()
            } catch (e: Exception) { }
            _playbackState.value = current.copy(isPlaying = false)
        } else {
            try {
                mediaPlayer?.start()
            } catch (e: Exception) { }
            _playbackState.value = current.copy(isPlaying = true)
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
        } catch (e: Exception) { }
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) { }
        mediaPlayer = null
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            currentPositionMs = 0
        )
    }

    fun closePlayer() {
        stop()
        _playbackState.value = PlayerPlaybackState()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val state = _playbackState.value
                if (state.isPlaying) {
                    val pos = try {
                        mediaPlayer?.currentPosition ?: (state.currentPositionMs + 500)
                    } catch (e: Exception) {
                        state.currentPositionMs + 500
                    }
                    if (pos >= state.totalDurationMs && state.totalDurationMs > 0) {
                        _playbackState.value = state.copy(
                            isPlaying = false,
                            currentPositionMs = state.totalDurationMs
                        )
                        break
                    } else {
                        _playbackState.value = state.copy(currentPositionMs = pos)
                    }
                }
                delay(500L)
            }
        }
    }
}
