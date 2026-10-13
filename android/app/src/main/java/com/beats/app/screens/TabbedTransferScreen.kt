package com.beats.app.screens

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beats.app.network.DiscoveredPeer
import com.beats.app.ui.theme.Accent
import com.beats.app.viewmodel.FileDownloadState
import com.beats.app.viewmodel.FileUploadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DeviceFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val dateModified: Long
)

private enum class FileTab(val title: String) {
    History("History"),
    Pictures("Photos"),
    Audio("Music"),
    Video("Videos"),
    Files("Files")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabbedTransferScreen(
    peer: DiscoveredPeer,
    uploads: List<FileUploadState>,
    downloads: List<FileDownloadState>,
    @Suppress("UNUSED_PARAMETER") isSending: Boolean,
    onUploadUris: (List<Uri>) -> Unit,
    onDisconnect: () -> Unit
) {
    val selectedFiles = remember { mutableStateListOf<Uri>() }
    val context = LocalContext.current
    val tabs = FileTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            if (selectedFiles.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        onUploadUris(selectedFiles.toList())
                        selectedFiles.clear()
                    },
                    containerColor = Accent,
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Upload"
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Accent)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Transfer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 0.dp,
                containerColor = Accent,
                contentColor = Color.White,
                divider = {},
                indicator = {
                    Divider(
                        color = Color.White,
                        thickness = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            selectedFiles.clear()
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            Text(
                                tab.title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (tabs[page]) {
                    FileTab.History -> HistoryTabContent(uploads, downloads)
                    FileTab.Pictures -> MediaGridContent(
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentResolver = context.contentResolver,
                        selectedFiles = selectedFiles,
                        showThumbnails = true
                    )
                    FileTab.Audio -> MediaListContent(
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        contentResolver = context.contentResolver,
                        selectedFiles = selectedFiles
                    )
                    FileTab.Video -> MediaGridContent(
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        contentResolver = context.contentResolver,
                        selectedFiles = selectedFiles,
                        showThumbnails = true
                    )
                    FileTab.Files -> MediaListContent(
                        contentUri = MediaStore.Files.getContentUri("external"),
                        contentResolver = context.contentResolver,
                        selectedFiles = selectedFiles
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTabContent(
    uploads: List<FileUploadState>,
    downloads: List<FileDownloadState>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (uploads.isNotEmpty()) {
            item {
                Text("Uploads", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(uploads) { file ->
                HistoryProgressItem(name = file.name, progress = file.progress, done = file.done)
            }
        }
        if (downloads.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Downloads", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(downloads) { file ->
                HistoryProgressItem(name = file.entry.name, progress = file.progress, done = file.done)
            }
        }
        if (uploads.isEmpty() && downloads.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No transfer history",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryProgressItem(name: String, progress: Float, done: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                if (done) {
                    Text("Done", fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                } else {
                    Text("${(progress * 100).toInt()}%", fontSize = 12.sp)
                }
            }
            if (!done) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = Accent
                )
            }
        }
    }
}

// Grid layout for Photos and Videos tabs
@Composable
private fun MediaGridContent(
    contentUri: Uri,
    contentResolver: ContentResolver,
    selectedFiles: MutableList<Uri>,
    showThumbnails: Boolean
) {
    var files by remember(contentUri) { mutableStateOf<List<DeviceFile>>(emptyList()) }
    var isLoading by remember(contentUri) { mutableStateOf(true) }

    LaunchedEffect(contentUri) {
        isLoading = true
        files = withContext(Dispatchers.IO) {
            queryMediaFiles(contentResolver, contentUri)
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        return
    }

    if (files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No files found", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        return
    }

    val grouped = files.groupBy { file ->
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(file.dateModified * 1000))
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp)
    ) {
        grouped.forEach { (date, dateFiles) ->
            item(span = { GridItemSpan(3) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            items(dateFiles, key = { it.uri.toString() }) { file ->
                val isSelected = selectedFiles.contains(file.uri)
                GridThumbnailItem(
                    file = file,
                    isSelected = isSelected,
                    showThumbnail = showThumbnails,
                    onClick = {
                        if (isSelected) selectedFiles.remove(file.uri)
                        else selectedFiles.add(file.uri)
                    }
                )
            }
        }
    }
}

@Composable
private fun GridThumbnailItem(
    file: DeviceFile,
    isSelected: Boolean,
    showThumbnail: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clickable(onClick = onClick)
    ) {
        if (showThumbnail) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(file.uri)
                    .crossfade(true)
                    .size(300)
                    .build(),
                contentDescription = file.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    file.name,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        // Circular selection indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Accent else Color.Black.copy(alpha = 0.3f)
                )
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// List layout for Audio and Files tabs
@Composable
private fun MediaListContent(
    contentUri: Uri,
    contentResolver: ContentResolver,
    selectedFiles: MutableList<Uri>
) {
    var files by remember(contentUri) { mutableStateOf<List<DeviceFile>>(emptyList()) }
    var isLoading by remember(contentUri) { mutableStateOf(true) }

    LaunchedEffect(contentUri) {
        isLoading = true
        files = withContext(Dispatchers.IO) {
            queryMediaFiles(contentResolver, contentUri)
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        return
    }

    if (files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No files found", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items(files, key = { it.uri.toString() }) { file ->
            val isSelected = selectedFiles.contains(file.uri)
            ListFileItem(
                file = file,
                isSelected = isSelected,
                onClick = {
                    if (isSelected) selectedFiles.remove(file.uri)
                    else selectedFiles.add(file.uri)
                }
            )
        }
    }
}

@Composable
private fun ListFileItem(
    file: DeviceFile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection circle
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) Accent else Color.Transparent)
                .border(1.5.dp, if (isSelected) Accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatFileSize(file.size),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

private fun queryMediaFiles(contentResolver: ContentResolver, contentUri: Uri): List<DeviceFile> {
    val files = mutableListOf<DeviceFile>()
    val projection = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.DATE_MODIFIED
    )
    val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

    contentResolver.query(contentUri, projection, null, null, sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

        var count = 0
        while (cursor.moveToNext() && count < 500) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn) ?: continue
            val size = cursor.getLong(sizeColumn)
            val dateModified = cursor.getLong(dateColumn)
            if (size <= 0) continue

            val uri = ContentUris.withAppendedId(contentUri, id)
            files.add(DeviceFile(uri = uri, name = name, size = size, dateModified = dateModified))
            count++
        }
    }
    return files
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
