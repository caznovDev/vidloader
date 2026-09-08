package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.download.DownloadEngine
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.model.MediaItem
import com.example.data.model.OutputFormat
import com.example.data.model.Playlist
import com.example.data.player.AudioPlayerManager
import com.example.data.player.PlayerPlaybackState
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab(val label: String) {
    EXPLORE("Explore"),
    SEARCH("Search"),
    PLAYLISTS("Playlists"),
    DOWNLOADS("Downloads")
}

data class FormatDialogState(
    val isOpen: Boolean = false,
    val targetItems: List<MediaItem> = emptyList(),
    val selectedFormat: OutputFormat = OutputFormat.MP3,
    val selectedQuality: String = "320 kbps",
    val autoConvert: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = MediaRepository()
    private val downloadEngine = DownloadEngine(application, database, viewModelScope)
    val audioPlayer = AudioPlayerManager(application, viewModelScope)

    // Navigation & Tabs
    private val _currentTab = MutableStateFlow(AppTab.EXPLORE)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Active Category Filter on Explore tab (All, Music, Videos)
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _recentSearches = MutableStateFlow(
        listOf("Shards of Glass", "Shape of You", "Megolamania", "Acoustic Sunset", "Lo-Fi Rain")
    )
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Playlists
    val playlists: List<Playlist> = repository.getPlaylists()
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    // Batch download selection mode
    private val _selectedMediaIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMediaIds: StateFlow<Set<String>> = _selectedMediaIds.asStateFlow()

    private val _isBatchModeActive = MutableStateFlow(false)
    val isBatchModeActive: StateFlow<Boolean> = _isBatchModeActive.asStateFlow()

    // Downloads from Engine
    val allDownloadTasks: StateFlow<List<DownloadTask>> = downloadEngine.tasks
        .map { it.values.toList().sortedByDescending { task -> task.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<List<DownloadTask>> = allDownloadTasks
        .map { list ->
            list.filter {
                it.status == DownloadStatus.DOWNLOADING ||
                it.status == DownloadStatus.QUEUED ||
                it.status == DownloadStatus.CONVERTING ||
                it.status == DownloadStatus.PAUSED
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedDownloads: StateFlow<List<DownloadTask>> = allDownloadTasks
        .map { list -> list.filter { it.status == DownloadStatus.COMPLETED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Format & Quality Selection Dialog
    private val _formatDialogState = MutableStateFlow(FormatDialogState())
    val formatDialogState: StateFlow<FormatDialogState> = _formatDialogState.asStateFlow()

    // Direct URL Dialog
    private val _showUrlDialog = MutableStateFlow(false)
    val showUrlDialog: StateFlow<Boolean> = _showUrlDialog.asStateFlow()

    val playbackState: StateFlow<PlayerPlaybackState> = audioPlayer.playbackState

    private var searchJob: Job? = null

    init {
        // Initial search results
        viewModelScope.launch {
            _searchResults.value = repository.getTrendingItems()
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = repository.getTrendingItems()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300L)
            _searchResults.value = repository.search(query)
            _isSearching.value = false
        }
    }

    fun performSearch(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank() && !_recentSearches.value.contains(query)) {
            _recentSearches.update { (listOf(query) + it).take(8) }
        }
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = repository.search(query)
            _isSearching.value = false
        }
    }

    fun clearRecentSearch(query: String) {
        _recentSearches.update { it.filter { item -> item != query } }
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
    }

    // Multi-select / Batch selection
    fun toggleBatchMode() {
        _isBatchModeActive.update { !it }
        if (!_isBatchModeActive.value) {
            _selectedMediaIds.value = emptySet()
        }
    }

    fun toggleItemSelection(mediaId: String) {
        _selectedMediaIds.update { set ->
            if (set.contains(mediaId)) set - mediaId else set + mediaId
        }
        if (_selectedMediaIds.value.isNotEmpty()) {
            _isBatchModeActive.value = true
        }
    }

    fun selectAll(items: List<MediaItem>) {
        _selectedMediaIds.value = items.map { it.id }.toSet()
        _isBatchModeActive.value = true
    }

    fun clearSelection() {
        _selectedMediaIds.value = emptySet()
        _isBatchModeActive.value = false
    }

    // Format Dialog
    fun openFormatDialogForSingle(media: MediaItem) {
        val defaultQuality = if (media.mediaType == com.example.data.model.MediaType.AUDIO) "320 kbps" else "1080p"
        _formatDialogState.value = FormatDialogState(
            isOpen = true,
            targetItems = listOf(media),
            selectedFormat = if (media.mediaType == com.example.data.model.MediaType.AUDIO) OutputFormat.MP3 else OutputFormat.MP4,
            selectedQuality = defaultQuality
        )
    }

    fun openFormatDialogForBatch(items: List<MediaItem>, defaultFormat: OutputFormat = OutputFormat.MP3) {
        val selected = items.filter { _selectedMediaIds.value.contains(it.id) }
        val target = if (selected.isNotEmpty()) selected else items
        val defaultQuality = if (defaultFormat.isAudioOnly) "320 kbps" else "1080p"
        _formatDialogState.value = FormatDialogState(
            isOpen = true,
            targetItems = target,
            selectedFormat = defaultFormat,
            selectedQuality = defaultQuality
        )
    }

    fun openFormatDialogForPlaylist(playlist: Playlist, defaultFormat: OutputFormat = OutputFormat.MP3) {
        val defaultQuality = if (defaultFormat.isAudioOnly) "320 kbps" else "1080p"
        _formatDialogState.value = FormatDialogState(
            isOpen = true,
            targetItems = playlist.tracks,
            selectedFormat = defaultFormat,
            selectedQuality = defaultQuality
        )
    }

    fun dismissFormatDialog() {
        _formatDialogState.value = FormatDialogState(isOpen = false)
    }

    fun updateDialogFormat(format: OutputFormat) {
        val current = _formatDialogState.value
        val newQuality = if (format.isAudioOnly) "320 kbps" else "1080p"
        _formatDialogState.value = current.copy(
            selectedFormat = format,
            selectedQuality = newQuality
        )
    }

    fun updateDialogQuality(quality: String) {
        _formatDialogState.value = _formatDialogState.value.copy(selectedQuality = quality)
    }

    fun confirmDownloadFromDialog() {
        val state = _formatDialogState.value
        if (state.targetItems.isEmpty()) return

        if (state.targetItems.size == 1) {
            downloadEngine.enqueue(state.targetItems.first(), state.selectedFormat, state.selectedQuality)
        } else {
            downloadEngine.enqueueBatch(state.targetItems, state.selectedFormat, state.selectedQuality)
        }

        clearSelection()
        dismissFormatDialog()
        // Switch to Downloads tab to see real-time progress!
        setTab(AppTab.DOWNLOADS)
    }

    // Direct URL Dialog
    fun setUrlDialogVisible(visible: Boolean) {
        _showUrlDialog.value = visible
    }

    fun downloadFromUrl(url: String, format: OutputFormat, quality: String) {
        val parsed = repository.parseUrl(url)
        downloadEngine.enqueue(parsed, format, quality)
        setUrlDialogVisible(false)
        setTab(AppTab.DOWNLOADS)
    }

    // Download controls
    fun pauseDownload(taskId: String) = downloadEngine.pauseDownload(taskId)
    fun resumeDownload(taskId: String) = downloadEngine.resumeDownload(taskId)
    fun cancelDownload(taskId: String) = downloadEngine.cancelDownload(taskId)
    fun retryDownload(taskId: String) = downloadEngine.retryDownload(taskId)
    fun deleteCompleted(taskId: String) = downloadEngine.deleteCompleted(taskId)
    fun clearAllCompleted() = downloadEngine.clearAllCompleted()
    fun pauseAll() = downloadEngine.pauseAll()
    fun resumeAll() = downloadEngine.resumeAll()

    // Player
    fun playMedia(task: DownloadTask) = audioPlayer.playMedia(task)
    fun togglePlayPause() = audioPlayer.togglePlayPause()
    fun seekTo(posMs: Int) = audioPlayer.seekTo(posMs)
    fun closePlayer() = audioPlayer.closePlayer()

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
