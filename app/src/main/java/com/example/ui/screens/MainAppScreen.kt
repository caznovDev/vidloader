package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OutputFormat
import com.example.ui.components.BatchActionBar
import com.example.ui.components.FormatSelectionBottomSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.UrlInputDialog
import com.example.ui.theme.AccentRed
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isBatchMode by viewModel.isBatchModeActive.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val activeTasks by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val completedTasks by viewModel.completedDownloads.collectAsStateWithLifecycle()
    val formatDialogState by viewModel.formatDialogState.collectAsStateWithLifecycle()
    val showUrlDialog by viewModel.showUrlDialog.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Explore Tab
                NavigationBarItem(
                    selected = currentTab == AppTab.EXPLORE,
                    onClick = { viewModel.setTab(AppTab.EXPLORE) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.EXPLORE) Icons.Filled.Explore else Icons.Outlined.Explore,
                            contentDescription = "Explore"
                        )
                    },
                    label = { Text("Explore", fontSize = 12.sp, fontWeight = if (currentTab == AppTab.EXPLORE) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentRed,
                        selectedTextColor = AccentRed,
                        indicatorColor = AccentRed.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_explore")
                )

                // Search Tab
                NavigationBarItem(
                    selected = currentTab == AppTab.SEARCH,
                    onClick = { viewModel.setTab(AppTab.SEARCH) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = "Search"
                        )
                    },
                    label = { Text("Search", fontSize = 12.sp, fontWeight = if (currentTab == AppTab.SEARCH) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentRed,
                        selectedTextColor = AccentRed,
                        indicatorColor = AccentRed.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_search")
                )

                // Playlists Tab
                NavigationBarItem(
                    selected = currentTab == AppTab.PLAYLISTS,
                    onClick = { viewModel.setTab(AppTab.PLAYLISTS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.PLAYLISTS) Icons.Filled.PlaylistPlay else Icons.Outlined.PlaylistPlay,
                            contentDescription = "Playlists"
                        )
                    },
                    label = { Text("Playlists", fontSize = 12.sp, fontWeight = if (currentTab == AppTab.PLAYLISTS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentRed,
                        selectedTextColor = AccentRed,
                        indicatorColor = AccentRed.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_playlists")
                )

                // Downloads Tab (with real-time active counter badge)
                NavigationBarItem(
                    selected = currentTab == AppTab.DOWNLOADS,
                    onClick = { viewModel.setTab(AppTab.DOWNLOADS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeTasks.isNotEmpty()) {
                                    Badge(
                                        containerColor = AccentRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("${activeTasks.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == AppTab.DOWNLOADS) Icons.Filled.CloudDownload else Icons.Outlined.CloudDownload,
                                contentDescription = "Downloads"
                            )
                        }
                    },
                    label = { Text("Downloads", fontSize = 12.sp, fontWeight = if (currentTab == AppTab.DOWNLOADS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentRed,
                        selectedTextColor = AccentRed,
                        indicatorColor = AccentRed.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_downloads")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Screen content with smooth crossfade
            Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                when (tab) {
                    AppTab.EXPLORE -> {
                        ExploreScreen(
                            viewModel = viewModel,
                            selectedCategory = selectedCategory,
                            onCategoryChange = { viewModel.setCategory(it) },
                            isBatchMode = isBatchMode,
                            selectedIds = selectedIds
                        )
                    }
                    AppTab.SEARCH -> {
                        SearchScreen(
                            viewModel = viewModel,
                            query = searchQuery,
                            searchResults = searchResults,
                            recentSearches = recentSearches,
                            isSearching = isSearching,
                            isBatchMode = isBatchMode,
                            selectedIds = selectedIds
                        )
                    }
                    AppTab.PLAYLISTS -> {
                        PlaylistsScreen(
                            viewModel = viewModel,
                            selectedPlaylist = selectedPlaylist,
                            isBatchMode = isBatchMode,
                            selectedIds = selectedIds
                        )
                    }
                    AppTab.DOWNLOADS -> {
                        DownloadsScreen(
                            viewModel = viewModel,
                            activeTasks = activeTasks,
                            completedTasks = completedTasks
                        )
                    }
                }
            }

            // Floating Batch Action Bar when items are selected
            val allItems = viewModel.playlists.flatMap { it.tracks }.distinctBy { it.id }
            val selectedItems = allItems.filter { selectedIds.contains(it.id) }

            BatchActionBar(
                selectedCount = selectedIds.size,
                totalCount = allItems.size,
                onSelectAll = { viewModel.selectAll(allItems) },
                onClear = { viewModel.clearSelection() },
                onBatchDownload = { format ->
                    viewModel.openFormatDialogForBatch(selectedItems, format)
                },
                onCustomBatchDownload = {
                    viewModel.openFormatDialogForBatch(selectedItems)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Mini Player Bar (shown when media playback is active)
            MiniPlayerBar(
                state = playbackState,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekTo(it) },
                onClose = { viewModel.closePlayer() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Format & Quality Selection Modal Bottom Sheet
            FormatSelectionBottomSheet(
                dialogState = formatDialogState,
                onFormatChange = { viewModel.updateDialogFormat(it) },
                onQualityChange = { viewModel.updateDialogQuality(it) },
                onConfirm = { viewModel.confirmDownloadFromDialog() },
                onDismiss = { viewModel.dismissFormatDialog() }
            )

            // Direct URL Input Dialog
            UrlInputDialog(
                isOpen = showUrlDialog,
                onDismiss = { viewModel.setUrlDialogVisible(false) },
                onDownload = { url, format, quality ->
                    viewModel.downloadFromUrl(url, format, quality)
                }
            )
        }
    }
}
