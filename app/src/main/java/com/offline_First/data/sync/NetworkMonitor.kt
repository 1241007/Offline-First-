package com.offline_First.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

class ConnectivityNetworkMonitor(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : NetworkMonitor {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        private val activeNetworks = mutableSetOf<Network>()

        override fun onAvailable(network: Network) {
            synchronized(this) {
                activeNetworks.add(network)
                updateState()
            }
        }

        override fun onLost(network: Network) {
            synchronized(this) {
                activeNetworks.remove(network)
                updateState()
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            synchronized(this) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (hasInternet) {
                    activeNetworks.add(network)
                } else {
                    activeNetworks.remove(network)
                }
                updateState()
            }
        }

        private fun updateState() {
            val connected = activeNetworks.isNotEmpty() || checkInitialConnectivity()
            scope.launch {
                if (_isOnline.value != connected) {
                    _isOnline.value = connected
                }
            }
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
            // Safe fallback for unit tests and environments where ConnectivityManager callback is not supported
        }
    }

    private fun checkInitialConnectivity(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
