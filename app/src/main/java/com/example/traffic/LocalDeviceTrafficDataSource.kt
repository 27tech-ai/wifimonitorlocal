package com.example.traffic

import android.net.TrafficStats
import com.example.model.MonitoringMode
import com.example.model.NetworkDevice

class LocalDeviceTrafficDataSource : TrafficDataSource {
    override val mode: MonitoringMode = MonitoringMode.LOCAL_DEVICE

    private var previousRxBytes: Long? = null
    private var previousTxBytes: Long? = null
    private var lastPollTime: Long = 0L

    private val localRxHistory = mutableListOf<Long>()
    private val localTxHistory = mutableListOf<Long>()

    override suspend fun pollTraffic(
        currentDevices: List<NetworkDevice>,
        elapsedMillis: Long
    ): List<NetworkDevice> {
        val now = System.currentTimeMillis()
        val currentRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
        val currentTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)

        val actualElapsed = if (lastPollTime > 0) now - lastPollTime else elapsedMillis
        lastPollTime = now

        val rxSpeed = TrafficCalculator.calculateSpeed(currentRx, previousRxBytes, actualElapsed)
        val txSpeed = TrafficCalculator.calculateSpeed(currentTx, previousTxBytes, actualElapsed)

        previousRxBytes = currentRx
        previousTxBytes = currentTx

        localRxHistory.add(rxSpeed)
        if (localRxHistory.size > 25) localRxHistory.removeAt(0)

        localTxHistory.add(txSpeed)
        if (localTxHistory.size > 25) localTxHistory.removeAt(0)

        return currentDevices.map { device ->
            if (device.isLocalDevice) {
                device.copy(
                    currentRxSpeed = rxSpeed,
                    currentTxSpeed = txSpeed,
                    totalRxBytes = currentRx,
                    totalTxBytes = currentTx,
                    lastSeen = now,
                    rxSpeedHistory = localRxHistory.toList(),
                    txSpeedHistory = localTxHistory.toList()
                )
            } else {
                // Cross-device network counters cannot be sniffed by non-root Android apps
                device.copy(
                    currentRxSpeed = 0L,
                    currentTxSpeed = 0L,
                    lastSeen = now
                )
            }
        }
    }

    override fun reset() {
        previousRxBytes = null
        previousTxBytes = null
        lastPollTime = 0L
        localRxHistory.clear()
        localTxHistory.clear()
    }
}
