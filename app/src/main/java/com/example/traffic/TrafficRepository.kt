package com.example.traffic

import android.content.Context
import com.example.discovery.LanScanner
import com.example.discovery.NetworkUtils
import com.example.model.MonitoringMode
import com.example.model.NetworkDevice
import com.example.model.NetworkSummary
import com.example.model.RouterConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TrafficRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val lanScanner = LanScanner(context)

    private val demoDataSource = DemoTrafficDataSource()
    private val localDeviceDataSource = LocalDeviceTrafficDataSource()
    private val routerDataSource = RouterOpenWrtTrafficDataSource()

    private var activeDataSource: TrafficDataSource = demoDataSource

    private val _devices = MutableStateFlow<List<NetworkDevice>>(emptyList())
    val devices: StateFlow<List<NetworkDevice>> = _devices.asStateFlow()

    private val _summary = MutableStateFlow(NetworkSummary())
    val summary: StateFlow<NetworkSummary> = _summary.asStateFlow()

    private val _currentMode = MutableStateFlow(MonitoringMode.DEMO)
    val currentMode: StateFlow<MonitoringMode> = _currentMode.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _routerErrorMessage = MutableStateFlow<String?>(null)
    val routerErrorMessage: StateFlow<String?> = _routerErrorMessage.asStateFlow()

    private var trafficPollJob: Job? = null
    private var scanJob: Job? = null
    private var monitoringStartTime: Long = 0L

    init {
        updateNetworkInfo()
    }

    fun setMode(mode: MonitoringMode) {
        _currentMode.value = mode
        activeDataSource = when (mode) {
            MonitoringMode.DEMO -> demoDataSource
            MonitoringMode.LOCAL_DEVICE -> localDeviceDataSource
            MonitoringMode.ROUTER_OPENWRT -> routerDataSource
        }
        activeDataSource.reset()
        _routerErrorMessage.value = null

        if (mode == MonitoringMode.DEMO) {
            // Pre-seed demo devices
            scope.launch {
                val demoDevs = demoDataSource.pollTraffic(emptyList(), 1000L)
                _devices.value = demoDevs
                updateSummary(demoDevs)
            }
        } else {
            // For real local or router mode, kick off quick scan
            startSubnetScan()
        }
    }

    fun configureRouter(config: RouterConfig) {
        routerDataSource.config = config
        routerDataSource.reset()
        _routerErrorMessage.value = null
    }

    fun startMonitoring() {
        if (trafficPollJob?.isActive == true) return

        monitoringStartTime = System.currentTimeMillis()
        _summary.value = _summary.value.copy(
            isMonitoring = true,
            monitoringSince = monitoringStartTime
        )

        trafficPollJob = scope.launch(Dispatchers.Default) {
            var lastTick = System.currentTimeMillis()
            while (isActive) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val elapsed = now - lastTick
                lastTick = now

                val currentList = _devices.value
                val updated = activeDataSource.pollTraffic(currentList, elapsed)
                _devices.value = updated
                updateSummary(updated)

                if (activeDataSource is RouterOpenWrtTrafficDataSource) {
                    _routerErrorMessage.value = routerDataSource.lastErrorMessage
                }
            }
        }
    }

    fun stopMonitoring() {
        trafficPollJob?.cancel()
        trafficPollJob = null
        _summary.value = _summary.value.copy(isMonitoring = false)
    }

    fun startSubnetScan() {
        if (_isScanning.value) return
        scanJob?.cancel()

        scanJob = scope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _scanProgress.value = 0f

            try {
                lanScanner.scanSubnetFlow().collect { progress ->
                    _scanProgress.value = progress.totalScanned.toFloat() / progress.totalTargets.toFloat()

                    // Merge newly discovered devices with existing
                    val existingMap = _devices.value.associateBy { it.ip }.toMutableMap()
                    progress.discoveredDevices.forEach { dev ->
                        val current = existingMap[dev.ip]
                        if (current != null) {
                            existingMap[dev.ip] = current.copy(
                                name = if (current.name.startsWith("Host-")) dev.name else current.name,
                                mac = current.mac ?: dev.mac,
                                vendor = current.vendor ?: dev.vendor,
                                lastSeen = System.currentTimeMillis()
                            )
                        } else {
                            existingMap[dev.ip] = dev
                        }
                    }

                    _devices.value = existingMap.values.toList()
                    updateSummary(_devices.value)

                    if (progress.isFinished) {
                        _isScanning.value = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun updateSummary(deviceList: List<NetworkDevice>) {
        val totalRxSpeed = deviceList.sumOf { it.currentRxSpeed }
        val totalTxSpeed = deviceList.sumOf { it.currentTxSpeed }
        val totalRx = deviceList.sumOf { it.totalRxBytes }
        val totalTx = deviceList.sumOf { it.totalTxBytes }

        _summary.value = _summary.value.copy(
            totalDevices = deviceList.size,
            activeDevices = deviceList.count { it.currentRxSpeed > 0 || it.currentTxSpeed > 0 },
            aggregateRxSpeed = totalRxSpeed,
            aggregateTxSpeed = totalTxSpeed,
            aggregateTotalRx = totalRx,
            aggregateTotalTx = totalTx
        )
    }

    fun updateNetworkInfo() {
        val subnetInfo = NetworkUtils.getLocalSubnetInfo(context)
        _summary.value = _summary.value.copy(
            subnet = "${subnetInfo?.subnetPrefix ?: "192.168.1."}0/24",
            localIp = subnetInfo?.localIp ?: "192.168.1.50",
            gatewayIp = subnetInfo?.gatewayIp ?: "192.168.1.1",
            wifiSsid = "Local Wi-Fi Network"
        )
    }
}
