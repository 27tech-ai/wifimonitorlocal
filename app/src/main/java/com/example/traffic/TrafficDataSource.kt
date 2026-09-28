package com.example.traffic

import com.example.model.MonitoringMode
import com.example.model.NetworkDevice

interface TrafficDataSource {
    val mode: MonitoringMode

    /**
     * Polls current traffic counters for devices and returns updated device list with speeds.
     * @param currentDevices Currently known devices
     * @param elapsedMillis Time elapsed since last poll in milliseconds (~1000ms)
     */
    suspend fun pollTraffic(
        currentDevices: List<NetworkDevice>,
        elapsedMillis: Long
    ): List<NetworkDevice>

    fun reset()
}
