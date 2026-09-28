package com.example.model

enum class DeviceType {
    PHONE,
    COMPUTER,
    ROUTER,
    TABLET,
    TV,
    IOT,
    UNKNOWN
}

enum class DeviceStatus {
    ACTIVE,
    IDLE,
    OFFLINE
}

data class NetworkDevice(
    val id: String, // IP or MAC
    val ip: String,
    val name: String,
    val mac: String? = null,
    val vendor: String? = null,
    val isLocalDevice: Boolean = false,
    val isGateway: Boolean = false,
    val status: DeviceStatus = DeviceStatus.ACTIVE,
    val currentRxSpeed: Long = 0L, // Bytes per second download
    val currentTxSpeed: Long = 0L, // Bytes per second upload
    val totalRxBytes: Long = 0L,   // Total downloaded
    val totalTxBytes: Long = 0L,   // Total uploaded
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val latencyMs: Long = 0L,
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val rxSpeedHistory: List<Long> = emptyList(), // last ~20 speed samples
    val txSpeedHistory: List<Long> = emptyList()
)
