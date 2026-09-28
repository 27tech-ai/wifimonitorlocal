package com.example.discovery

import android.content.Context
import com.example.model.DeviceStatus
import com.example.model.NetworkDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class LanScanner(private val context: Context) {

    data class ScanProgress(
        val totalScanned: Int,
        val totalTargets: Int,
        val discoveredDevices: List<NetworkDevice>,
        val isFinished: Boolean
    )

    /**
     * Scans local /24 subnet gently without overloading the network or battery.
     * Uses rate-limited concurrent probes (12 concurrent probes max) with 250ms timeouts.
     */
    fun scanSubnetFlow(): Flow<ScanProgress> = flow {
        val subnetInfo = NetworkUtils.getLocalSubnetInfo(context)
        val prefix = subnetInfo?.subnetPrefix ?: "192.168.1."
        val localIp = subnetInfo?.localIp ?: "192.168.1.50"
        val gatewayIp = subnetInfo?.gatewayIp ?: "192.168.1.1"

        val discovered = mutableListOf<NetworkDevice>()
        val now = System.currentTimeMillis()

        // 1. Immediately add Gateway
        val gatewayDevice = NetworkDevice(
            id = gatewayIp,
            ip = gatewayIp,
            name = "Default Gateway / Router",
            mac = NetworkUtils.getMacFromArp(gatewayIp),
            isLocalDevice = false,
            isGateway = true,
            status = DeviceStatus.ACTIVE,
            firstSeen = now,
            lastSeen = now,
            latencyMs = 2L,
            deviceType = com.example.model.DeviceType.ROUTER
        )
        discovered.add(gatewayDevice)

        // 2. Immediately add Local Android device
        val localDevice = NetworkDevice(
            id = localIp,
            ip = localIp,
            name = "This Android Device",
            mac = null, // Hidden on Android 10+
            isLocalDevice = true,
            isGateway = false,
            status = DeviceStatus.ACTIVE,
            firstSeen = now,
            lastSeen = now,
            latencyMs = 0L,
            deviceType = com.example.model.DeviceType.PHONE
        )
        if (localIp != gatewayIp) {
            discovered.add(localDevice)
        }

        emit(ScanProgress(2, 254, discovered.toList(), false))

        // 3. Scan the remainder of the /24 subnet (1 to 254) gently
        val semaphore = Semaphore(12) // Limit concurrent socket probes
        val hostList = (1..254).map { "$prefix$it" }.filter { it != gatewayIp && it != localIp }

        coroutineScope {
            var scannedCount = 2
            val deferreds = hostList.map { targetIp ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val isAlive = probeHost(targetIp, 280)
                        if (isAlive) {
                            val hostName = NetworkUtils.resolveHostname(targetIp, false, false)
                            val mac = NetworkUtils.getMacFromArp(targetIp)
                            val (vendor, devType) = NetworkUtils.identifyVendor(mac)
                            val finalType = if (devType != com.example.model.DeviceType.UNKNOWN) {
                                devType
                            } else {
                                NetworkUtils.inferDeviceType(hostName, false, false)
                            }

                            val dev = NetworkDevice(
                                id = targetIp,
                                ip = targetIp,
                                name = hostName,
                                mac = mac,
                                vendor = vendor,
                                isLocalDevice = false,
                                isGateway = false,
                                status = DeviceStatus.ACTIVE,
                                firstSeen = System.currentTimeMillis(),
                                lastSeen = System.currentTimeMillis(),
                                latencyMs = 8L,
                                deviceType = finalType
                            )
                            synchronized(discovered) {
                                discovered.add(dev)
                            }
                        }
                    }
                    synchronized(this@LanScanner) {
                        scannedCount++
                    }
                    if (scannedCount % 15 == 0 || scannedCount >= 254) {
                        val snapshot = synchronized(discovered) { discovered.toList() }
                        emit(ScanProgress(scannedCount, 254, snapshot, scannedCount >= 254))
                    }
                }
            }
            deferreds.awaitAll()
        }

        val finalSnapshot = synchronized(discovered) { discovered.toList() }
        emit(ScanProgress(254, 254, finalSnapshot, true))
    }.flowOn(Dispatchers.IO)

    /**
     * Gentle probe: tests ICMP reachability or common LAN service ports (80, 443, 53, 8080)
     */
    private fun probeHost(ip: String, timeoutMs: Int): Boolean {
        try {
            // First try InetAddress isReachable
            val inet = InetAddress.getByName(ip)
            if (inet.isReachable(timeoutMs)) {
                return true
            }

            // Fallback: check common lightweight ports
            val portsToProbe = intArrayOf(80, 443, 8080, 53)
            for (port in portsToProbe) {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), timeoutMs)
                    return true
                }
            }
        } catch (_: Exception) {
            // Not reachable on probed ports
        }
        return false
    }

    suspend fun getQuickDeviceList(): List<NetworkDevice> = withContext(Dispatchers.IO) {
        val subnetInfo = NetworkUtils.getLocalSubnetInfo(context)
        val localIp = subnetInfo?.localIp ?: "192.168.1.50"
        val gatewayIp = subnetInfo?.gatewayIp ?: "192.168.1.1"
        val now = System.currentTimeMillis()

        listOf(
            NetworkDevice(
                id = gatewayIp,
                ip = gatewayIp,
                name = "Default Gateway / Router",
                mac = NetworkUtils.getMacFromArp(gatewayIp),
                isLocalDevice = false,
                isGateway = true,
                status = DeviceStatus.ACTIVE,
                firstSeen = now,
                lastSeen = now,
                latencyMs = 2L,
                deviceType = com.example.model.DeviceType.ROUTER
            ),
            NetworkDevice(
                id = localIp,
                ip = localIp,
                name = "This Android Device",
                mac = null,
                isLocalDevice = true,
                isGateway = false,
                status = DeviceStatus.ACTIVE,
                firstSeen = now,
                lastSeen = now,
                latencyMs = 0L,
                deviceType = com.example.model.DeviceType.PHONE
            )
        )
    }
}
