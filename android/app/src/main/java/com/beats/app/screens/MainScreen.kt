package com.beats.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beats.app.R
import com.beats.app.components.PulsingCirclesAnimation
import com.beats.app.network.ConnectionStatus
import com.beats.app.network.DiscoveredPeer
import com.beats.app.ui.theme.Accent
import com.beats.app.viewmodel.FileDownloadState
import com.beats.app.viewmodel.FileUploadState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    connectionStatus: ConnectionStatus,
    discoveredPeers: List<DiscoveredPeer>,
    connectedPeer: DiscoveredPeer?,
    uploads: List<FileUploadState>,
    downloads: List<FileDownloadState>,
    isSending: Boolean,
    onPeerClick: (DiscoveredPeer) -> Unit,
    onScanClick: () -> Unit,
    onSelectFiles: () -> Unit,
    onSend: () -> Unit,
    onDisconnect: () -> Unit,
    wifiSsid: String,
    wifiPassword: String
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val showSheet = remember { mutableStateOf(connectionStatus == ConnectionStatus.Disconnected) }

    if (showSheet.value && connectionStatus == ConnectionStatus.Disconnected) {
        ModalBottomSheet(
            onDismissRequest = { showSheet.value = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Connect to Wifi Hotspot",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Text(text = "SSID: $wifiSsid", fontSize = 16.sp)
                    Text(text = "Password: $wifiPassword", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            showSheet.value = false
                        }
                    },
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Accent),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(top = 12.dp)
                        .height(48.dp)
                ) {
                    Text("Dismiss")
                }
            }
        }
    }

    when (connectionStatus) {
        ConnectionStatus.Connected -> {
            if (connectedPeer != null) {
                TransferScreen(
                    peer = connectedPeer,
                    uploads = uploads,
                    downloads = downloads,
                    isSending = isSending,
                    onSelectFiles = onSelectFiles,
                    onSend = onSend,
                    onDisconnect = onDisconnect
                )
            }
        }
        else -> {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        showSheet.value = true
                        coroutineScope.launch { sheetState.show() }
                    }
            ) {
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(bottom = 50.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                PulsingCirclesAnimation()

                                Spacer(modifier = Modifier.height(24.dp))

                                if (connectionStatus == ConnectionStatus.Connecting) {
                                    Text(
                                        "Connecting...",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else if (discoveredPeers.isNotEmpty()) {
                                    Text(
                                        "Devices found",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                        items(discoveredPeers) { peer ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable { onPeerClick(peer) },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            peer.name,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            "${peer.ip}:${peer.port}",
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                    Text("Connect", color = Accent, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        "Searching for devices...",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (connectionStatus == ConnectionStatus.Error) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Connection failed. Try again.",
                                        fontSize = 14.sp,
                                        color = Color.Red
                                    )
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 48.dp)
                        ) {
                            Button(
                                onClick = onScanClick,
                                shape = RoundedCornerShape(5.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Accent,
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Accent),
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .padding(top = 12.dp)
                                    .height(48.dp)
                                    .shadow(8.dp, RoundedCornerShape(50), clip = false)
                            ) {
                                Text("Scan QR code", fontSize = 14.sp)
                                Image(
                                    painter = painterResource(id = R.drawable.qr_code),
                                    contentDescription = "QR Code Icon",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
