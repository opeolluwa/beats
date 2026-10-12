package com.beats.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beats.app.hooks.APManager
import com.beats.app.network.ConnectionStatus
import com.beats.app.network.ServiceDiscovery
import com.beats.app.screens.MainScreen
import com.beats.app.ui.theme.BeatsTheme
import com.beats.app.viewmodel.TransferViewModel

class MainActivity : ComponentActivity() {

    private lateinit var apManager: APManager
    private lateinit var serviceDiscovery: ServiceDiscovery
    private val selectedFileUris = mutableStateOf<List<Uri>>(emptyList())
    private val isBroadcasting = mutableStateOf(false)
    private val hotspotSsid = mutableStateOf("")
    private val hotspotPassword = mutableStateOf("")

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            selectedFileUris.value = uris
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private var viewModelRef: TransferViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        apManager = APManager.getApManager(this)
        serviceDiscovery = ServiceDiscovery(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        checkFileAccessPermission()

        setContent {
            BeatsTheme {
                val vm: TransferViewModel = viewModel()
                viewModelRef = vm

                val connectionStatus by vm.connectionStatus.collectAsState()
                val connectedPeer by vm.connectedPeer.collectAsState()
                val uploads by vm.uploads.collectAsState()
                val downloads by vm.downloads.collectAsState()
                val isSending by vm.isSending.collectAsState()
                val peers by serviceDiscovery.peers.collectAsState()
                val pendingUris = selectedFileUris.value

                if (pendingUris.isNotEmpty() && connectionStatus == ConnectionStatus.Connected) {
                    vm.uploadFiles(pendingUris, contentResolver)
                    selectedFileUris.value = emptyList()
                }

                MainScreen(
                    connectionStatus = connectionStatus,
                    discoveredPeers = peers,
                    connectedPeer = connectedPeer,
                    uploads = uploads,
                    downloads = downloads,
                    isSending = isSending,
                    isBroadcasting = isBroadcasting.value,
                    hotspotSsid = hotspotSsid.value,
                    hotspotPassword = hotspotPassword.value,
                    onPeerClick = { peer -> vm.connectToPeer(peer) },
                    onBroadcastClick = { startBroadcast() },
                    onJoinClick = { joinNetwork() },
                    onRefreshClick = { refreshDiscovery() },
                    onSelectFiles = { filePickerLauncher.launch(arrayOf("*/*")) },
                    onSend = {
                        if (pendingUris.isNotEmpty()) {
                            vm.uploadFiles(pendingUris, contentResolver)
                            selectedFileUris.value = emptyList()
                        }
                    },
                    onDisconnect = { vm.disconnect() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceDiscovery.stop()
    }

    private fun refreshDiscovery() {
        serviceDiscovery.stop()
        serviceDiscovery.register(18005)
        serviceDiscovery.startDiscovery()
        Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
    }

    private fun joinNetwork() {
        if (!apManager.isDeviceConnectedToWifi) {
            Toast.makeText(this, "Connect to a WiFi network first", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            return
        }

        isBroadcasting.value = true
        serviceDiscovery.register(18005)
        serviceDiscovery.startDiscovery()
    }

    private fun startBroadcast() {
        if (apManager.isDeviceConnectedToWifi) {
            val wifiManager = apManager.wifiManager
            @Suppress("DEPRECATION")
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                wifiManager.isWifiEnabled = false
                Toast.makeText(this, "Turning off WiFi...", Toast.LENGTH_SHORT).show()
                android.os.Handler(mainLooper).postDelayed({ startBroadcast() }, 1500)
            } else {
                Toast.makeText(this, "Please turn off WiFi to broadcast", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            return
        }

        if (apManager.isWifiApEnabled) {
            apManager.disableWifiAp()
            Toast.makeText(this, "Turning off existing hotspot...", Toast.LENGTH_SHORT).show()
            android.os.Handler(mainLooper).postDelayed({ startBroadcast() }, 1500)
            return
        }

        turnOnHotspot()
    }

    private val storagePermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false

        if (readGranted && writeGranted) {
            Toast.makeText(this, "Storage permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Storage permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkFileAccessPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } else {
            storagePermissionRequest.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun turnOnHotspot() {
        apManager.turnOnHotspot(
            this,
            { ssid, password ->
                Log.d("Hotspot", "SSID: $ssid, Password: $password")
                hotspotSsid.value = ssid
                hotspotPassword.value = password
                isBroadcasting.value = true
                serviceDiscovery.register(18005)
                serviceDiscovery.startDiscovery()
            }
        ) { failureCode, e ->
            val message = when (failureCode) {
                APManager.ERROR_GPS_PROVIDER_DISABLED -> "Enable GPS to start hotspot"
                APManager.ERROR_LOCATION_PERMISSION_DENIED -> "Grant location permission"
                APManager.ERROR_DISABLE_HOTSPOT -> "Disable existing hotspot first"
                APManager.ERROR_DISABLE_WIFI -> "Turn off Wi-Fi to create hotspot"
                APManager.ERROR_WRITE_SETTINGS_PERMISSION_REQUIRED -> "Allow modify system settings"
                else -> "Unknown error: ${e?.message}"
            }

            Toast.makeText(this@MainActivity, "Hotspot failed: $message", Toast.LENGTH_LONG)
                .show()
            e?.printStackTrace()
        }
    }
}
