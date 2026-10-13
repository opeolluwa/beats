package com.beats.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beats.app.network.DiscoveredPeer
import com.beats.app.ui.theme.Accent
import com.beats.app.viewmodel.FileDownloadState
import com.beats.app.viewmodel.FileUploadState

@Composable
fun TransferScreen(
    peer: DiscoveredPeer,
    uploads: List<FileUploadState>,
    downloads: List<FileDownloadState>,
    isSending: Boolean,
    onSelectFiles: () -> Unit,
    onSend: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Connected to ${peer.name}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDisconnect) {
                Text("Disconnect", color = Color.Red)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Send Files", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onSelectFiles,
                enabled = !isSending,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.weight(1f)
            ) {
                Text("Select Files")
            }
            if (uploads.any { !it.done }) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSend,
                    enabled = !isSending,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("Send")
                }
            }
        }

        if (uploads.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 200.dp)) {
                items(uploads) { file ->
                    FileProgressItem(name = file.name, progress = file.progress, done = file.done)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Received Files", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (downloads.isEmpty()) {
            Text(
                "Listening for files...",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(downloads) { file ->
                    FileProgressItem(
                        name = file.entry.name,
                        progress = file.progress,
                        done = file.done
                    )
                }
            }
        }
    }
}

@Composable
private fun FileProgressItem(name: String, progress: Float, done: Boolean) {
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
