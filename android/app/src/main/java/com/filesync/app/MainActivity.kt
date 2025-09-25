package com.filesync.app

import com.filesync.app.screens.MainScreen
import android.Manifest
import android.content.Context
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
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.filesync.app.hooks.APManager
import com.filesync.app.ui.theme.FileSyncAndroidTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {

    private lateinit var apManager: APManager
    private val wifiSsid = mutableStateOf("")
    private val wifiPassword = mutableStateOf("")


    // Location Permission Request
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkWriteSettingsPermission()
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        apManager = APManager.getApManager(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkWriteSettingsPermission()
        }
        checkFileAccessPermission()
        setContent {
            FileSyncAndroidTheme {
                MainScreen(
                    wifiSsid = wifiSsid.value,
                    wifiPassword = wifiPassword.value
                )
            }
        }
    }


    private val storagePermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false

        if (readGranted && writeGranted) {
            Toast.makeText(this, "Storage permissions granted", Toast.LENGTH_SHORT).show()
            // Proceed with file operations
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
            } else {
                Toast.makeText(this, "All file access granted", Toast.LENGTH_SHORT).show()
                // Proceed with file operations
            }
        } else {
            // Android 10 and below
            storagePermissionRequest.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }


    private fun checkWriteSettingsPermission() {
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } else {
            turnOnHotspot()
        }
    }


    private fun turnOnHotspot() {
        apManager.turnOnHotspot(
            this,
            { ssid, password ->
                Log.d("Hotspot", "SSID: $ssid, Password: $password")
                wifiSsid.value = ssid
                wifiPassword.value = password
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
