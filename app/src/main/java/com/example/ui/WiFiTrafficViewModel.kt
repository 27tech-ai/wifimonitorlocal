package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.MonitoringMode
import com.example.model.NetworkDevice
import com.example.model.NetworkSummary
import com.example.model.RouterConfig
import com.example.model.SortOption
import com.example.traffic.TrafficRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WiFiTrafficUiState(
    val devices: List<NetworkDevice> = emptyList(),
    val summary: NetworkSummary = NetworkSummary(),
    val currentMode: MonitoringMode = MonitoringMode.DEMO,
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DOWNLOAD_SPEED,
    val selectedDevice: NetworkDevice? = null,
    val showRouterConfigDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val showFirstLaunchBanner: Boolean = true,
    val routerConfig: RouterConfig = RouterConfig(),
    val routerError: String? = null
)

class WiFiTrafficViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TrafficRepository(application, viewModelScope)

    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.DOWNLOAD_SPEED)
    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    private val _showRouterDialog = MutableStateFlow(false)
    private val _showPrivacyDialog = MutableStateFlow(false)
    private val _showFirstLaunchBanner = MutableStateFlow(true)
    private val _routerConfig = MutableStateFlow(RouterConfig())

    val uiState: StateFlow<WiFiTrafficUiState> = combine(
        repository.devices,
        repository.summary,
        repository.currentMode,
        repository.isScanning,
        repository.scanProgress,
        repository.routerErrorMessage,
        _searchQuery,
        _sortOption,
        _selectedDeviceId,
        _showRouterDialog,
        _showPrivacyDialog,
        _showFirstLaunchBanner,
        _routerConfig
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val rawDevices = params[0] as List<NetworkDevice>
        val summary = params[1] as NetworkSummary
        val mode = params[2] as MonitoringMode
        val isScanning = params[3] as Boolean
        val scanProgress = params[4] as Float
        val routerError = params[5] as String?
        val query = params[6] as String
        val sort = params[7] as SortOption
        val selectedId = params[8] as String?
        val showRouter = params[9] as Boolean
        val showPrivacy = params[10] as Boolean
        val showFirstLaunch = params[11] as Boolean
        val rConfig = params[12] as RouterConfig

        // Filter by search query
        val filtered = if (query.isBlank()) {
            rawDevices
        } else {
            val q = query.trim().lowercase()
            rawDevices.filter { dev ->
                dev.name.lowercase().contains(q) ||
                dev.ip.contains(q) ||
                (dev.mac?.lowercase()?.contains(q) == true) ||
                (dev.vendor?.lowercase()?.contains(q) == true)
            }
        }

        // Sort
        val sorted = when (sort) {
            SortOption.DOWNLOAD_SPEED -> filtered.sortedByDescending { it.currentRxSpeed }
            SortOption.UPLOAD_SPEED -> filtered.sortedByDescending { it.currentTxSpeed }
            SortOption.DEVICE_NAME -> filtered.sortedBy { it.name.lowercase() }
            SortOption.IP_ADDRESS -> filtered.sortedWith(Comparator { a, b -> compareIps(a.ip, b.ip) })
            SortOption.TOTAL_DATA -> filtered.sortedByDescending { it.totalRxBytes + it.totalTxBytes }
        }

        val selectedDev = if (selectedId != null) {
            rawDevices.find { it.id == selectedId }
        } else null

        WiFiTrafficUiState(
            devices = sorted,
            summary = summary,
            currentMode = mode,
            isScanning = isScanning,
            scanProgress = scanProgress,
            searchQuery = query,
            sortOption = sort,
            selectedDevice = selectedDev,
            showRouterConfigDialog = showRouter,
            showPrivacyDialog = showPrivacy,
            showFirstLaunchBanner = showFirstLaunch,
            routerConfig = rConfig,
            routerError = routerError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WiFiTrafficUiState()
    )

    init {
        // Start in Demo mode immediately so user sees live discovery & traffic right away
        repository.setMode(MonitoringMode.DEMO)
        repository.startMonitoring()
    }

    fun toggleMonitoring() {
        if (uiState.value.summary.isMonitoring) {
            repository.stopMonitoring()
        } else {
            repository.startMonitoring()
        }
    }

    fun setMonitoringMode(mode: MonitoringMode) {
        repository.setMode(mode)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun rescanNetwork() {
        repository.startSubnetScan()
    }

    fun selectDevice(device: NetworkDevice?) {
        _selectedDeviceId.value = device?.id
    }

    fun dismissFirstLaunchBanner() {
        _showFirstLaunchBanner.value = false
    }

    fun showRouterConfigDialog(show: Boolean) {
        _showRouterDialog.value = show
    }

    fun showPrivacyDialog(show: Boolean) {
        _showPrivacyDialog.value = show
    }

    fun updateRouterConfig(config: RouterConfig) {
        _routerConfig.value = config
        repository.configureRouter(config)
        if (uiState.value.currentMode == MonitoringMode.ROUTER_OPENWRT) {
            repository.setMode(MonitoringMode.ROUTER_OPENWRT)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopMonitoring()
    }

    private fun compareIps(ip1: String, ip2: String): Int {
        val parts1 = ip1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = ip2.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until minOf(parts1.size, parts2.size)) {
            val cmp = parts1[i].compareTo(parts2[i])
            if (cmp != 0) return cmp
        }
        return ip1.compareTo(ip2)
    }
}
