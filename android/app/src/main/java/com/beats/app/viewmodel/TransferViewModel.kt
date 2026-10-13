package com.beats.app.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beats.app.network.ConnectionStatus
import com.beats.app.network.DiscoveredPeer
import com.beats.app.network.FileSyncClient
import com.beats.app.network.OutboxEntry
import com.beats.app.network.ServerConnection
import com.beats.app.network.ServiceDiscovery
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class FileUploadState(val uri: Uri, val name: String, val progress: Float, val done: Boolean)
data class FileDownloadState(val entry: OutboxEntry, val progress: Float, val done: Boolean)

class TransferViewModel : ViewModel() {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedPeer = MutableStateFlow<DiscoveredPeer?>(null)
    val connectedPeer: StateFlow<DiscoveredPeer?> = _connectedPeer.asStateFlow()

    private val _uploads = MutableStateFlow<List<FileUploadState>>(emptyList())
    val uploads: StateFlow<List<FileUploadState>> = _uploads.asStateFlow()

    private val _downloads = MutableStateFlow<List<FileDownloadState>>(emptyList())
    val downloads: StateFlow<List<FileDownloadState>> = _downloads.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var client: FileSyncClient? = null
    private var pollingJob: Job? = null

    fun connectToPeer(peer: DiscoveredPeer) {
        _connectionStatus.value = ConnectionStatus.Connecting
        val connection = ServerConnection(peer.ip, peer.port)
        client = FileSyncClient(connection)

        viewModelScope.launch {
            val reachable = client?.ping() == true
            if (reachable) {
                _connectionStatus.value = ConnectionStatus.Connected
                _connectedPeer.value = peer
                startReceivePolling()
            } else {
                _connectionStatus.value = ConnectionStatus.Error
                client = null
            }
        }
    }

    fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null
        client = null
        _connectionStatus.value = ConnectionStatus.Disconnected
        _connectedPeer.value = null
        _uploads.value = emptyList()
        _downloads.value = emptyList()
    }

    fun uploadFiles(uris: List<Uri>, contentResolver: ContentResolver) {
        val syncClient = client ?: return
        _isSending.value = true

        val states = uris.map { uri ->
            var name = "unknown"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
            }
            FileUploadState(uri = uri, name = name, progress = 0f, done = false)
        }
        _uploads.value = states

        viewModelScope.launch {
            for ((index, state) in states.withIndex()) {
                val result = syncClient.uploadFile(state.uri, contentResolver) { progress ->
                    _uploads.value = _uploads.value.toMutableList().also {
                        it[index] = it[index].copy(progress = progress)
                    }
                }
                _uploads.value = _uploads.value.toMutableList().also {
                    it[index] = it[index].copy(progress = 1f, done = result.isSuccess)
                }
            }
            _isSending.value = false
        }
    }

    private fun startReceivePolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val destDir = File(
                Environment.getExternalStorageDirectory(),
                "Beats"
            )
            while (true) {
                delay(3000)
                val syncClient = client ?: break
                val result = syncClient.listOutbox()
                val entries = result.getOrNull() ?: continue
                if (entries.isEmpty()) continue

                for (entry in entries) {
                    val alreadyDownloading = _downloads.value.any { it.entry.id == entry.id }
                    if (alreadyDownloading) continue

                    _downloads.value = _downloads.value + FileDownloadState(entry, 0f, false)
                    val dlIndex = _downloads.value.size - 1

                    val dlResult = syncClient.downloadFile(entry.path, destDir) { progress ->
                        _downloads.value = _downloads.value.toMutableList().also {
                            if (dlIndex < it.size) it[dlIndex] = it[dlIndex].copy(progress = progress)
                        }
                    }

                    _downloads.value = _downloads.value.toMutableList().also {
                        if (dlIndex < it.size) it[dlIndex] = it[dlIndex].copy(progress = 1f, done = dlResult.isSuccess)
                    }

                    if (dlResult.isSuccess) {
                        syncClient.ackOutbox(entry.id)
                    }
                }
            }
        }
    }
}
