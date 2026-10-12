package com.beats.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beats.app.components.RadarDiscoveryView
import com.beats.app.network.ConnectionStatus
import com.beats.app.network.DiscoveredPeer
import com.beats.app.ui.theme.Accent
import com.beats.app.viewmodel.FileDownloadState
import com.beats.app.viewmodel.FileUploadState

@Composable
fun MainScreen(
    connectionStatus: ConnectionStatus,
    discoveredPeers: List<DiscoveredPeer>,
    connectedPeer: DiscoveredPeer?,
    uploads: List<FileUploadState>,
    downloads: List<FileDownloadState>,
    isSending: Boolean,
    isBroadcasting: Boolean,
    hotspotSsid: String,
    hotspotPassword: String,
    onPeerClick: (DiscoveredPeer) -> Unit,
    onBroadcastClick: () -> Unit,
    onJoinClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSelectFiles: () -> Unit,
    onSend: () -> Unit,
    onDisconnect: () -> Unit
) {
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
                modifier = Modifier.fillMaxSize()
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
                                if (isBroadcasting) {
                                    RadarDiscoveryView(
                                        peers = discoveredPeers,
                                        onPeerClick = onPeerClick
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (connectionStatus == ConnectionStatus.Connecting) {
                                        Text(
                                            "Connecting...",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else if (discoveredPeers.isEmpty()) {
                                        Text(
                                            if (hotspotSsid.isNotEmpty()) "Broadcasting..." else "Searching...",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    if (hotspotSsid.isNotEmpty() && discoveredPeers.isEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth(0.85f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Text(
                                                    "Network",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                                Text(
                                                    hotspotSsid,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "Password",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                                Text(
                                                    hotspotPassword,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
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
                        }

                        if (isBroadcasting) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 48.dp)
                            ) {
                                Button(
                                    onClick = onRefreshClick,
                                    shape = RoundedCornerShape(5.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Accent,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(48.dp)
                                ) {
                                    Text("Refresh", fontSize = 14.sp)
                                }
                            }
                        }

                        if (!isBroadcasting) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 48.dp)
                            ) {
                                Button(
                                    onClick = onBroadcastClick,
                                    shape = RoundedCornerShape(5.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Accent,
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Accent),
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(48.dp)
                                        .shadow(8.dp, RoundedCornerShape(50), clip = false)
                                ) {
                                    Text("Broadcast", fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = onJoinClick,
                                    shape = RoundedCornerShape(5.dp),
                                    border = BorderStroke(1.dp, Accent),
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(48.dp)
                                ) {
                                    Text("Join", fontSize = 14.sp, color = Accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
