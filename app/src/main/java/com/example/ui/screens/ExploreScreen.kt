package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.data.model.OutputFormat
import com.example.ui.components.MediaItemCard
import com.example.ui.theme.AccentRed
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ExploreScreen(
    viewModel: MainViewModel,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    isBatchMode: Boolean,
    selectedIds: Set<String>,
    modifier: Modifier = Modifier
) {
    val allItems = viewModel.playlists.flatMap { it.tracks }.distinctBy { it.id }
    val filteredItems = when (selectedCategory) {
        "Music (MP3)" -> allItems.filter { it.mediaType == com.example.data.model.MediaType.AUDIO }
        "Videos (MP4)" -> allItems.filter { it.mediaType == com.example.data.model.MediaType.VIDEO }
        else -> allItems
    }

    val trendingToday = filteredItems.take(4)
    val popularList = filteredItems.drop(2).take(4)
    val newestList = filteredItems.asReversed().take(4)

    val heroItem = allItems.firstOrNull()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("explore_list"),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "YTDL",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "nis",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = AccentRed
                            )
                        }
                        Text(
                            text = "Batch Downloader & Converter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Multi-selection batch mode toggle button
                        IconButton(
                            onClick = { viewModel.toggleBatchMode() },
                            modifier = Modifier.testTag("toggle_batch_mode_btn")
                        ) {
                            Icon(
                                imageVector = if (isBatchMode) Icons.Filled.Checklist else Icons.Outlined.Checklist,
                                contentDescription = "Batch Select Mode",
                                tint = if (isBatchMode) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Search shortcut
                        IconButton(
                            onClick = { viewModel.setTab(AppTab.SEARCH) },
                            modifier = Modifier.testTag("open_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Link download dialog
                        IconButton(
                            onClick = { viewModel.setUrlDialogVisible(true) },
                            modifier = Modifier.testTag("open_link_dialog_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Download Link",
                                tint = AccentRed
                            )
                        }
                    }
                }
            }

            // Categories Filter Chips
            item {
                val categories = listOf("All", "Music (MP3)", "Videos (MP4)", "Trending")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategoryChange(category) },
                            label = {
                                Text(
                                    text = category,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentRed,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Hero Featured Card
            if (heroItem != null && selectedCategory == "All") {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { viewModel.openFormatDialogForSingle(heroItem) }
                            .testTag("featured_hero_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                            AsyncImage(
                                model = heroItem.thumbnailUrl,
                                contentDescription = heroItem.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Gradient Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                            startY = 50f
                                        )
                                    )
                            )

                            // Content overlay
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AccentRed
                                ) {
                                    Text(
                                        text = "FEATURED RELEASE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = heroItem.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${heroItem.channel} • ${heroItem.views} • ${heroItem.duration}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            // Quick Download Pill Button
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .clickable { viewModel.openFormatDialogForSingle(heroItem) },
                                shape = CircleShape,
                                color = AccentRed
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Download",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TRENDING TODAY Header (From Mockup)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRENDING TODAY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isBatchMode) {
                        Text(
                            text = "Select all",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.selectAll(trendingToday) }
                        )
                    }
                }
            }

            // Trending List Items
            items(trendingToday, key = { "trend_${it.id}" }) { item ->
                MediaItemCard(
                    media = item,
                    isSelected = selectedIds.contains(item.id),
                    isBatchMode = isBatchMode,
                    onToggleSelect = { viewModel.toggleItemSelection(item.id) },
                    onClick = { viewModel.openFormatDialogForSingle(item) },
                    onQuickDownload = { format ->
                        val quality = if (format.isAudioOnly) "320 kbps" else "1080p"
                        viewModel.openFormatDialogForSingle(item)
                    },
                    onCustomDownload = { viewModel.openFormatDialogForSingle(item) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }

            // POPULAR Header (From Mockup)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "POPULAR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Batch download",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            viewModel.selectAll(popularList)
                        }
                    )
                }
            }

            // Popular List Items
            items(popularList, key = { "pop_${it.id}" }) { item ->
                MediaItemCard(
                    media = item,
                    isSelected = selectedIds.contains(item.id),
                    isBatchMode = isBatchMode,
                    onToggleSelect = { viewModel.toggleItemSelection(item.id) },
                    onClick = { viewModel.openFormatDialogForSingle(item) },
                    onQuickDownload = { format -> viewModel.openFormatDialogForSingle(item) },
                    onCustomDownload = { viewModel.openFormatDialogForSingle(item) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }

            // NEWEST Header (From Mockup)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "NEWEST",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            items(newestList, key = { "new_${it.id}" }) { item ->
                MediaItemCard(
                    media = item,
                    isSelected = selectedIds.contains(item.id),
                    isBatchMode = isBatchMode,
                    onToggleSelect = { viewModel.toggleItemSelection(item.id) },
                    onClick = { viewModel.openFormatDialogForSingle(item) },
                    onQuickDownload = { format -> viewModel.openFormatDialogForSingle(item) },
                    onCustomDownload = { viewModel.openFormatDialogForSingle(item) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }
        }

        // Circular Red FAB (Direct match to uploaded screenshots!)
        FloatingActionButton(
            onClick = { viewModel.setUrlDialogVisible(true) },
            containerColor = AccentRed,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
                .size(56.dp)
                .testTag("floating_add_download_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Download URL",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
