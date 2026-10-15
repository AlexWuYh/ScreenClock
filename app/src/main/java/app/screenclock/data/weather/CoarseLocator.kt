package app.screenclock.data.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import android.content.pm.PackageManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class LatLon(val latitude: Double, val longitude: Double)

interface CoarseLocator {
    /** One coarse fix, or null when the device cannot provide one. */
    suspend fun currentCoarse(): LatLon?
}

/**
 * One coarse fix. Network provider first. On API 31+, fused is the fallback and stays coarse
 * because this app never asks for fine location. No GPS, and no reverse geocoding: Open-Meteo
 * cannot turn a coordinate into a city name, and no other host is allowed.
 */
class AndroidCoarseLocator(private val context: Context) : CoarseLocator {
    override suspend fun currentCoarse(): LatLon? {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val provider = coarseProvider(manager) ?: return null
        return withTimeoutOrNull(12_000) { awaitFix(manager, provider) }
    }

    private fun coarseProvider(manager: LocationManager): String? {
        if (providerEnabled(manager, LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            providerEnabled(manager, LocationManager.FUSED_PROVIDER)
        ) {
            return LocationManager.FUSED_PROVIDER
        }
        return null
    }

    private fun providerEnabled(manager: LocationManager, name: String): Boolean {
        return try {
            manager.isProviderEnabled(name)
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun awaitFix(manager: LocationManager, provider: String): LatLon? {
        return suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val cancel = CancellationSignal()
                continuation.invokeOnCancellation { cancel.cancel() }
                manager.getCurrentLocation(provider, cancel, context.mainExecutor) { location ->
                    if (continuation.isActive) continuation.resume(location.toLatLon())
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location.toLatLon())
                    }

                    @Deprecated("Deprecated in the platform listener.")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) = Unit
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                try {
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                } catch (_: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                } catch (_: IllegalArgumentException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }
}

private fun Location?.toLatLon(): LatLon? {
    if (this == null) return null
    return LatLon(latitude, longitude)
}
