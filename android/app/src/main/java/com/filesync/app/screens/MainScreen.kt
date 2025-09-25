package com.filesync.app.screens

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filesync.app.R
import com.filesync.app.components.PulsingCirclesAnimation
import com.filesync.app.ui.theme.Accent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(wifiSsid: String, wifiPassword: String) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val showSheet = remember { mutableStateOf(true) }

    // Show bottom sheet if triggered
    if (showSheet.value) {
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
                Column(modifier = Modifier) {
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
                    // Uncomment if you want an icon:
                    // Image(
                    //     painter = painterResource(id = R.drawable.qr_code),
                    //     contentDescription = "QR Code Icon",
                    //     modifier = Modifier.size(24.dp)
                    // )
                }
            }
        }
    }

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
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp)
                ) {
                    Text(
                        "Waiting for connection...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
