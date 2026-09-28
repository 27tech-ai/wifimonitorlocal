package com.example.traffic

import com.example.discovery.NetworkUtils
import com.example.model.DeviceStatus
import com.example.model.MonitoringMode
import com.example.model.NetworkDevice
import com.example.model.RouterClientPayload
import com.example.model.RouterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class RouterOpenWrtTrafficDataSource(
    var config: RouterConfig = RouterConfig()
) : TrafficDataSource {
    override val mode: MonitoringMode = MonitoringMode.ROUTER_OPENWRT

    var lastErrorMessage: String? = null
        private set

    var lastSuccessfulSync: Long = 0L
        private set

    private val client = OkHttpClient.Builder()
        .connectTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
        .readTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
        .build()

    private val previousCounters = mutableMapOf<String, CounterBaseline>()

    private data class CounterBaseline(
        val rxBytes: Long,
        val txBytes: Long,
        val timestamp: Long,
        val rxHistory: MutableList<Long> = mutableListOf(),
        val txHistory: MutableList<Long> = mutableListOf()
    )

    override suspend fun pollTraffic(
        currentDevices: List<NetworkDevice>,
        elapsedMillis: Long
    ): List<NetworkDevice> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        try {
            val requestBuilder = Request.Builder()
                .url(config.endpointUrl)
                .get()

            if (config.apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                lastErrorMessage = "Router returned HTTP ${response.code}: ${response.message}"
                return@withContext currentDevices
            }

            val body = response.body?.string() ?: ""
            val clients = parseRouterResponse(body)
            lastErrorMessage = null
            lastSuccessfulSync = now

            val deviceMap = currentDevices.associateBy { it.ip }.toMutableMap()

            clients.forEach { payload ->
                val ip = payload.ip
                val prev = previousCounters[ip]

                val actualElapsed = if (prev != null && prev.timestamp > 0) {
                    now - prev.timestamp
                } else {
                    elapsedMillis
                }

                val rxSpeed = TrafficCalculator.calculateSpeed(payload.rx_bytes, prev?.rxBytes, actualElapsed)
                val txSpeed = TrafficCalculator.calculateSpeed(payload.tx_bytes, prev?.txBytes, actualElapsed)

                val rxHist = prev?.rxHistory ?: mutableListOf()
                val txHist = prev?.txHistory ?: mutableListOf()

                rxHist.add(rxSpeed)
                if (rxHist.size > 25) rxHist.removeAt(0)

                txHist.add(txSpeed)
                if (txHist.size > 25) txHist.removeAt(0)

                previousCounters[ip] = CounterBaseline(
                    rxBytes = payload.rx_bytes,
                    txBytes = payload.tx_bytes,
                    timestamp = now,
                    rxHistory = rxHist,
                    txHistory = txHist
                )

                val existing = deviceMap[ip]
                val mac = payload.mac ?: existing?.mac
                val (vendor, devType) = NetworkUtils.identifyVendor(mac)
                val hostname = payload.hostname ?: existing?.name ?: NetworkUtils.resolveHostname(ip, false, false)
                val finalType = if (devType != com.example.model.DeviceType.UNKNOWN) {
                    devType
                } else {
                    NetworkUtils.inferDeviceType(hostname, ip.endsWith(".1"), false)
                }

                val updatedDevice = NetworkDevice(
                    id = ip,
                    ip = ip,
                    name = hostname,
                    mac = mac,
                    vendor = vendor ?: existing?.vendor,
                    isLocalDevice = existing?.isLocalDevice ?: false,
                    isGateway = existing?.isGateway ?: ip.endsWith(".1"),
                    status = DeviceStatus.ACTIVE,
                    currentRxSpeed = rxSpeed,
                    currentTxSpeed = txSpeed,
                    totalRxBytes = payload.rx_bytes,
                    totalTxBytes = payload.tx_bytes,
                    firstSeen = existing?.firstSeen ?: now,
                    lastSeen = now,
                    latencyMs = existing?.latencyMs ?: 5L,
                    deviceType = finalType,
                    rxSpeedHistory = rxHist.toList(),
                    txSpeedHistory = txHist.toList()
                )
                deviceMap[ip] = updatedDevice
            }

            return@withContext deviceMap.values.toList()
        } catch (e: IOException) {
            lastErrorMessage = "Connection error: ${e.localizedMessage ?: "Could not reach router"}"
            return@withContext currentDevices
        } catch (e: Exception) {
            lastErrorMessage = "Error parsing response: ${e.localizedMessage}"
            return@withContext currentDevices
        }
    }

    private fun parseRouterResponse(jsonString: String): List<RouterClientPayload> {
        val trimmed = jsonString.trim()
        val result = mutableListOf<RouterClientPayload>()

        if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            if (root.has("clients")) {
                val array = root.getJSONArray("clients")
                for (i in 0 until array.length()) {
                    parseClientObject(array.getJSONObject(i))?.let { result.add(it) }
                }
            }
        } else if (trimmed.startsWith("[")) {
            val array = JSONArray(trimmed)
            for (i in 0 until array.length()) {
                parseClientObject(array.getJSONObject(i))?.let { result.add(it) }
            }
        }
        return result
    }

    private fun parseClientObject(obj: JSONObject): RouterClientPayload? {
        val ip = obj.optString("ip").takeIf { it.isNotBlank() } ?: return null
        val mac = obj.optString("mac").takeIf { it.isNotBlank() }
        val hostname = obj.optString("hostname").takeIf { it.isNotBlank() }
        val rx = obj.optLong("rx_bytes", 0L)
        val tx = obj.optLong("tx_bytes", 0L)
        val ts = obj.optLong("timestamp", 0L)

        return RouterClientPayload(
            ip = ip,
            mac = mac,
            hostname = hostname,
            rx_bytes = rx,
            tx_bytes = tx,
            timestamp = ts
        )
    }

    override fun reset() {
        previousCounters.clear()
        lastErrorMessage = null
        lastSuccessfulSync = 0L
    }
}
