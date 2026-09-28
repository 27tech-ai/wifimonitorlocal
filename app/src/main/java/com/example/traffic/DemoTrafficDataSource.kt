package com.example.traffic

import com.example.model.DeviceStatus
import com.example.model.DeviceType
import com.example.model.MonitoringMode
import com.example.model.NetworkDevice
import kotlin.random.Random

class DemoTrafficDataSource : TrafficDataSource {
    override val mode: MonitoringMode = MonitoringMode.DEMO

    private val deviceStateMap = mutableMapOf<String, DeviceTrafficState>()

    private data class DeviceTrafficState(
        var totalRx: Long,
        var totalTx: Long,
        var baseRxSpeed: Long,
        var baseTxSpeed: Long,
        val rxHistory: MutableList<Long> = mutableListOf(),
        val txHistory: MutableList<Long> = mutableListOf()
    )

    init {
        initDefaultStates()
    }

    private fun initDefaultStates() {
        deviceStateMap.clear()
        val now = System.currentTimeMillis()

        // Samsung (from user prompt example: ~2.4 MB/s, 120 KB/s)
        deviceStateMap["192.168.1.5"] = DeviceTrafficState(
            totalRx = 2_840_000_000L,
            totalTx = 142_000_000L,
            baseRxSpeed = 2_450_000L,
            baseTxSpeed = 122_880L
        )

        // PC (from user prompt example: ~350 KB/s, 40 KB/s)
        deviceStateMap["192.168.1.10"] = DeviceTrafficState(
            totalRx = 890_000_000L,
            totalTx = 210_000_000L,
            baseRxSpeed = 358_400L,
            baseTxSpeed = 40_960L
        )

        // Unknown IoT (from user prompt example: 0 KB/s, 2 KB/s)
        deviceStateMap["192.168.1.15"] = DeviceTrafficState(
            totalRx = 12_400_000L,
            totalTx = 38_000_000L,
            baseRxSpeed = 400L,
            baseTxSpeed = 2_048L
        )

        // MacBook Pro
        deviceStateMap["192.168.1.18"] = DeviceTrafficState(
            totalRx = 3_120_000_000L,
            totalTx = 480_000_000L,
            baseRxSpeed = 1_150_000L,
            baseTxSpeed = 256_000L
        )

        // Smart TV 4K
        deviceStateMap["192.168.1.25"] = DeviceTrafficState(
            totalRx = 6_450_000_000L,
            totalTx = 18_000_000L,
            baseRxSpeed = 4_200_000L,
            baseTxSpeed = 16_384L
        )

        // Raspberry Pi Server
        deviceStateMap["192.168.1.30"] = DeviceTrafficState(
            totalRx = 410_000_000L,
            totalTx = 1_250_000_000L,
            baseRxSpeed = 85_000L,
            baseTxSpeed = 634_880L
        )

        // This Android Device
        deviceStateMap["192.168.1.50"] = DeviceTrafficState(
            totalRx = 640_000_000L,
            totalTx = 85_000_000L,
            baseRxSpeed = 184_000L,
            baseTxSpeed = 36_000L
        )

        // Default Gateway
        deviceStateMap["192.168.1.1"] = DeviceTrafficState(
            totalRx = 14_362_400_000L,
            totalTx = 2_141_000_000L,
            baseRxSpeed = 8_427_800L,
            baseTxSpeed = 1_106_264L
        )
    }

    override suspend fun pollTraffic(
        currentDevices: List<NetworkDevice>,
        elapsedMillis: Long
    ): List<NetworkDevice> {
        val seconds = (elapsedMillis.toDouble() / 1000.0).coerceAtLeast(0.5)

        // If list is empty, supply initial demo devices
        val baseDevices = if (currentDevices.isEmpty()) {
            createInitialDemoDevices()
        } else {
            currentDevices
        }

        return baseDevices.map { device ->
            val state = deviceStateMap.getOrPut(device.ip) {
                DeviceTrafficState(
                    totalRx = (10_000_000L..500_000_000L).random(),
                    totalTx = (1_000_000L..50_000_000L).random(),
                    baseRxSpeed = (10_000L..300_000L).random(),
                    baseTxSpeed = (2_000L..40_000L).random()
                )
            }

            // Introduce realistic natural fluctuation (+/- 18%)
            val rxJitter = 0.82 + (Random.nextDouble() * 0.36)
            val txJitter = 0.82 + (Random.nextDouble() * 0.36)

            val currentRxSpeed = (state.baseRxSpeed * rxJitter).toLong().coerceAtLeast(0L)
            val currentTxSpeed = (state.baseTxSpeed * txJitter).toLong().coerceAtLeast(0L)

            // Increment cumulative bytes
            val deltaRx = (currentRxSpeed * seconds).toLong()
            val deltaTx = (currentTxSpeed * seconds).toLong()
            state.totalRx += deltaRx
            state.totalTx += deltaTx

            // Update mini history (keep last 25 points for graphs)
            state.rxHistory.add(currentRxSpeed)
            if (state.rxHistory.size > 25) state.rxHistory.removeAt(0)

            state.txHistory.add(currentTxSpeed)
            if (state.txHistory.size > 25) state.txHistory.removeAt(0)

            device.copy(
                currentRxSpeed = currentRxSpeed,
                currentTxSpeed = currentTxSpeed,
                totalRxBytes = state.totalRx,
                totalTxBytes = state.totalTx,
                lastSeen = System.currentTimeMillis(),
                rxSpeedHistory = state.rxHistory.toList(),
                txSpeedHistory = state.txHistory.toList()
            )
        }
    }

    private fun createInitialDemoDevices(): List<NetworkDevice> {
        val now = System.currentTimeMillis()
        return listOf(
            NetworkDevice(
                id = "192.168.1.5",
                ip = "192.168.1.5",
                name = "Samsung Galaxy S24",
                mac = "50:F0:D3:4A:2B:11",
                vendor = "Samsung Electronics",
                isLocalDevice = false,
                isGateway = false,
                status = DeviceStatus.ACTIVE,
                firstSeen = now - 3600_000,
                lastSeen = now,
                latencyMs = 9L,
                deviceType = DeviceType.PHONE
            ),
            NetworkDevice(
                id = "192.168.1.10",
                ip = "192.168.1.10",
                name = "Workstation PC",
                mac = "80:86:00:1E:99:C4",
                vendor = "Intel Corp",
                isLocalDevice = false,
                isGateway = false,
                status = DeviceStatus.ACTIVE,
                firstSeen = now - 7200_000,
                lastSeen = now,
                latencyMs = 4L,
                deviceType = DeviceType.COMPUTER
            ),
            NetworkDevice(
                id = "192.168.1.15",
                ip = "192.168.1.15",
                name = "Unknown IoT Device",
                mac = "24:0A:C4:B8:33:F1",
                vendor = "Espressif IoT",
                isLocalDevice = false,
                isGateway = false,
                status = DeviceStatus.ACTIVE,
                firstSeen = now - 1800_000,
                lastSeen = now,
                latencyMs = 18L,
                deviceType = DeviceType.IOT
            ),
            NetworkDevice(
                id = "192.168.1.18",
                ip = "192.168.1.18",
                name = "MacBook Pro M3",
                mac = "FC:FC:48:88:22:90",
                vendor = "Apple Inc.",
                isLocalDevice = false,
                isGateway = false,
                status = DeviceStatus.ACTIVE,
                firstSeen = now - 5400_000,
                lastSeen = now,
                latencyMs = 6L,
                deviceType = DeviceType.COMPUTER
            ),
            NetworkDevice(
                id = "192.168.1.25",
                ip = "192.168.1.25",
                name = "Smart TV 4K",
                mac = "A4:83:E7:55:10:44",
                vendor = "Sony Bravia",
                isLocalDevice = false,
                isGateway = false,
                status = DeviceStatus.ACTIVE,
                firstSeen = now - 14400_000,
                lastSeen = now,
                latencyMs = 12L,
                deviceType = DeviceType.TV
            ),
            NetworkDevice(
                id = "192.168.1.30",
                ip = "192.168.1.30",
                name = "Raspberry Pi Server",
                mac = "B8:27:EB:12:44:A8",
                vendor = "Raspberry Pi",
                isLocalDevice = false,
                isGateway = false,
                status = DeviceStatus.ACTIVE,
                firstSeen = now - 86400_000,
                lastSeen = now,
                latencyMs = 3L,
                deviceType = DeviceType.COMPUTER
            ),
            NetworkDevice(
                id = "192.168.1.50",
                ip = "192.168.1.50",
                name = "This Android Device",
                mac = null, // Hidden on Android 10+
                vendor = "Google",
                isLocalDevice = true,
                isGateway = false,
                status = DeviceStatus.ACTIVE,
                firstSeen = now - 900_000,
                lastSeen = now,
                latencyMs = 0L,
                deviceType = DeviceType.PHONE
            ),
            NetworkDevice(
                id = "192.168.1.1",
                ip = "192.168.1.1",
                name = "Default Gateway / Router",
                mac = "98:48:27:A1:00:01",
                vendor = "TP-Link",
                isLocalDevice = false,
                isGateway = true,
                status = DeviceStatus.ACTIVE,
                firstSeen = now - 86400_000,
                lastSeen = now,
                latencyMs = 1L,
                deviceType = DeviceType.ROUTER
            )
        )
    }

    override fun reset() {
        initDefaultStates()
    }
}
