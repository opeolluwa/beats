package com.beats.app.network

data class ServerConnection(val ip: String, val port: Int) {
    val baseUrl: String get() = "http://$ip:$port"
}

enum class ConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Error
}
