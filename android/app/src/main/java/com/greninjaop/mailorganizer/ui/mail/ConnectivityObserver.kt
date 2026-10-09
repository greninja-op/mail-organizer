package com.greninjaop.mailorganizer.ui.mail

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Representation of device network connectivity state (Phase 19 §15, §16).
 * Distinguishes online, offline, and metered network state.
 */
data class NetworkState(
    val isOnline: Boolean,
    val isMetered: Boolean = false,
)

/**
 * Offline and network awareness interface (Phase 6, Phase 19 §15, §16).
 *
 * Browsing synchronized mail never needs the network; this drives the
 * honest "You're offline" banner, background sync gating, and network recovery.
 * Kept behind an interface so ViewModels and schedulers stay unit-testable without Robolectric.
 */
interface ConnectivityObserver {
    val isOnline: Flow<Boolean>
    val networkState: Flow<NetworkState> get() = isOnline.map { NetworkState(it) }
}

/** Android implementation backed by [ConnectivityManager]. */
class AndroidConnectivityObserver(appContext: Context) : ConnectivityObserver {

    private val manager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private fun checkState(): NetworkState {
        val network = manager.activeNetwork ?: return NetworkState(isOnline = false, isMetered = false)
        val caps = manager.getNetworkCapabilities(network) ?: return NetworkState(isOnline = false, isMetered = false)
        val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isNotMetered = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return NetworkState(isOnline = online, isMetered = !isNotMetered)
    }

    override val networkState: Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(checkState())
            }

            override fun onLost(network: Network) {
                trySend(checkState())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                caps: NetworkCapabilities,
            ) {
                trySend(checkState())
            }
        }
        trySend(checkState())
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    override val isOnline: Flow<Boolean> = networkState.map { it.isOnline }.distinctUntilChanged()
}
