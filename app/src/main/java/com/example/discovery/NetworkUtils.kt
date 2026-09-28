package com.example.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.example.model.DeviceType
import java.io.BufferedReader
import java.io.FileReader
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

object NetworkUtils {

    data class SubnetInfo(
        val localIp: String,
        val gatewayIp: String,
        val subnetPrefix: String, // e.g. "192.168.1."
        val prefixLength: Int,   // e.g. 24
        val interfaceName: String
    )

    fun getLocalSubnetInfo(context: Context): SubnetInfo? {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

            // Try getting Gateway from WifiManager DHCP
            var gatewayIp = "192.168.1.1"
            if (wifiManager != null) {
                @Suppress("DEPRECATION")
                val dhcpInfo = wifiManager.dhcpInfo
                if (dhcpInfo != null && dhcpInfo.gateway != 0) {
                    val g = dhcpInfo.gateway
                    gatewayIp = "${g and 0xFF}.${(g shr 8) and 0xFF}.${(g shr 16) and 0xFF}.${(g shr 24) and 0xFF}"
                }
            }

            // Find non-loopback active IPv4 interface (wlan0 or eth0 or similar)
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                // Prefer wlan, eth, or any up interface
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress ?: continue
                        // Compute prefix
                        val lastDot = hostAddress.lastIndexOf('.')
                        if (lastDot != -1) {
                            val prefix = hostAddress.substring(0, lastDot + 1)
                            return SubnetInfo(
                                localIp = hostAddress,
                                gatewayIp = gatewayIp,
                                subnetPrefix = prefix,
                                prefixLength = 24,
                                interfaceName = intf.name
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback for emulator / default environment
        return SubnetInfo(
            localIp = "192.168.1.50",
            gatewayIp = "192.168.1.1",
            subnetPrefix = "192.168.1.",
            prefixLength = 24,
            interfaceName = "wlan0"
        )
    }

    /**
     * Attempts to read MAC address from /proc/net/arp.
     * Note: Android 10+ restricts non-root app access to /proc/net/arp.
     * Returns null if blocked or unavailable.
     */
    fun getMacFromArp(ip: String): String? {
        var bufferedReader: BufferedReader? = null
        try {
            bufferedReader = BufferedReader(FileReader("/proc/net/arp"))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                val splitted = line!!.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (splitted.size >= 4 && splitted[0] == ip) {
                    val mac = splitted[3]
                    if (mac != "00:00:00:00:00:00" && mac.matches("..:..:..:..:..:..".toRegex())) {
                        return mac.uppercase(Locale.US)
                    }
                }
            }
        } catch (_: Exception) {
            // Permission denied or file not readable on Android 10+
        } finally {
            try {
                bufferedReader?.close()
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Resolves hostname for an IP address.
     * First checks reverse DNS / NetBIOS, falls back to friendly name.
     */
    fun resolveHostname(ip: String, isLocal: Boolean, isGateway: Boolean): String {
        if (isLocal) return "This Android Device"
        if (isGateway) return "Default Gateway / Router"

        try {
            val inetAddress = InetAddress.getByName(ip)
            val hostName = inetAddress.canonicalHostName
            if (!hostName.isNullOrEmpty() && hostName != ip && !hostName.startsWith("192.") && !hostName.startsWith("10.")) {
                return hostName.substringBefore('.')
            }
        } catch (_: Exception) {}

        return "Host-$ip"
    }

    /**
     * Identifies device vendor and general device type from MAC OUI if available
     */
    fun identifyVendor(mac: String?): Pair<String?, DeviceType> {
        if (mac.isNullOrEmpty() || mac == "02:00:00:00:00:00") {
            return Pair(null, DeviceType.UNKNOWN)
        }

        val cleanMac = mac.replace("[:-]".toRegex(), "").uppercase(Locale.US)
        if (cleanMac.length < 6) return Pair(null, DeviceType.UNKNOWN)

        val oui = cleanMac.substring(0, 6)

        return when {
            // Apple
            oui.startsWith("0017F2") || oui.startsWith("001EC2") || oui.startsWith("ACFDCE") ||
            oui.startsWith("3C15C2") || oui.startsWith("B8E856") || oui.startsWith("FCFC48") ||
            oui.startsWith("A483E7") || oui.startsWith("BC9FEF") -> Pair("Apple Inc.", DeviceType.PHONE)

            // Samsung
            oui.startsWith("001247") || oui.startsWith("00166C") || oui.startsWith("50F0D3") ||
            oui.startsWith("842519") || oui.startsWith("E458E7") || oui.startsWith("A89FBA") -> Pair("Samsung Electronics", DeviceType.PHONE)

            // Google
            oui.startsWith("F4F5D8") || oui.startsWith("D46A91") || oui.startsWith("001A11") ||
            oui.startsWith("3C5AB4") -> Pair("Google", DeviceType.PHONE)

            // Intel
            oui.startsWith("0002B3") || oui.startsWith("001302") || oui.startsWith("0013E8") ||
            oui.startsWith("34E6D7") || oui.startsWith("808600") -> Pair("Intel Corp", DeviceType.COMPUTER)

            // Espressif (IoT / Smart home)
            oui.startsWith("240AC4") || oui.startsWith("30AEA4") || oui.startsWith("A4CF12") ||
            oui.startsWith("ECFABC") -> Pair("Espressif IoT", DeviceType.IOT)

            // Raspberry Pi
            oui.startsWith("B827EB") || oui.startsWith("DCA632") || oui.startsWith("E45F01") -> Pair("Raspberry Pi", DeviceType.COMPUTER)

            // TP-Link
            oui.startsWith("001D0F") || oui.startsWith("14CC20") || oui.startsWith("50C7BF") ||
            oui.startsWith("984827") -> Pair("TP-Link", DeviceType.ROUTER)

            // Netgear
            oui.startsWith("00095B") || oui.startsWith("00146C") || oui.startsWith("04A151") -> Pair("Netgear", DeviceType.ROUTER)

            else -> Pair("Network Device", DeviceType.UNKNOWN)
        }
    }

    fun inferDeviceType(name: String, isGateway: Boolean, isLocal: Boolean): DeviceType {
        if (isGateway) return DeviceType.ROUTER
        if (isLocal) return DeviceType.PHONE

        val lower = name.lowercase(Locale.US)
        return when {
            lower.contains("router") || lower.contains("gateway") || lower.contains("ap") || lower.contains("openwrt") -> DeviceType.ROUTER
            lower.contains("samsung") || lower.contains("pixel") || lower.contains("iphone") || lower.contains("galaxy") || lower.contains("phone") || lower.contains("android") -> DeviceType.PHONE
            lower.contains("pc") || lower.contains("macbook") || lower.contains("laptop") || lower.contains("desktop") || lower.contains("thinkpad") || lower.contains("surface") || lower.contains("ubuntu") || lower.contains("windows") -> DeviceType.COMPUTER
            lower.contains("ipad") || lower.contains("tablet") -> DeviceType.TABLET
            lower.contains("tv") || lower.contains("roku") || lower.contains("chromecast") || lower.contains("firestick") || lower.contains("bravia") -> DeviceType.TV
            lower.contains("esp") || lower.contains("smart") || lower.contains("bulb") || lower.contains("cam") || lower.contains("sensor") || lower.contains("hub") -> DeviceType.IOT
            else -> DeviceType.UNKNOWN
        }
    }
}
