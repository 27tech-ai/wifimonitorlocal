package com.example.model

enum class MonitoringMode(
    val title: String,
    val description: String
) {
    DEMO(
        title = "Demo / Simulated LAN",
        description = "Simulates realistic multi-device Wi-Fi traffic across multiple clients for testing without router configuration."
    ),
    LOCAL_DEVICE(
        title = "Local Device (TrafficStats)",
        description = "Monitors this Android device's live network traffic via Android TrafficStats API, with subnet device discovery."
    ),
    ROUTER_OPENWRT(
        title = "OpenWrt / Router Endpoint",
        description = "Connects to your local OpenWrt router or custom LAN gateway API to collect per-device interface RX/TX counters."
    )
}

enum class SortOption(val displayName: String) {
    DOWNLOAD_SPEED("Download Speed"),
    UPLOAD_SPEED("Upload Speed"),
    DEVICE_NAME("Device Name"),
    IP_ADDRESS("IP Address"),
    TOTAL_DATA("Total Data")
}

data class RouterConfig(
    val endpointUrl: String = "http://192.168.1.1/api/traffic",
    val apiKey: String = "",
    val timeoutSeconds: Int = 3
)

data class NetworkSummary(
    val totalDevices: Int = 0,
    val activeDevices: Int = 0,
    val aggregateRxSpeed: Long = 0L,
    val aggregateTxSpeed: Long = 0L,
    val aggregateTotalRx: Long = 0L,
    val aggregateTotalTx: Long = 0L,
    val subnet: String = "192.168.1.0/24",
    val localIp: String = "127.0.0.1",
    val gatewayIp: String = "192.168.1.1",
    val wifiSsid: String = "Wi-Fi LAN",
    val monitoringSince: Long = 0L,
    val isMonitoring: Boolean = false
)

/**
 * Expected JSON payload from OpenWrt or local router endpoint:
 * {
 *   "router_name": "OpenWrt Router",
 *   "timestamp": 1720000000,
 *   "clients": [
 *     {
 *       "ip": "192.168.1.5",
 *       "mac": "AA:BB:CC:DD:EE:01",
 *       "hostname": "Samsung-S23",
 *       "rx_bytes": 104857600,
 *       "tx_bytes": 12582912,
 *       "timestamp": 1720000000
 *     }
 *   ]
 * }
 */
data class RouterClientPayload(
    val ip: String,
    val mac: String? = null,
    val hostname: String? = null,
    val rx_bytes: Long = 0L,
    val tx_bytes: Long = 0L,
    val timestamp: Long? = null
)

data class RouterTrafficResponse(
    val router_name: String? = null,
    val timestamp: Long? = null,
    val clients: List<RouterClientPayload> = emptyList()
)
