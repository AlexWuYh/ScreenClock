package app.screenclock.data.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import android.content.pm.PackageManager
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val FIX_TIMEOUT_MILLIS = 8_000L
private const val GEOCODE_TIMEOUT_MILLIS = 5_000L

/** [name] is the city the system resolved for the fix, when it could. */
data class LatLon(val latitude: Double, val longitude: Double, val name: String? = null)

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
        // A live fix from each coarse provider in turn, then the newest fix the system already
        // holds. Some phones (no Google network location) answer one provider with nothing.
        for (provider in coarseProviders(manager)) {
            val fix = withTimeoutOrNull(FIX_TIMEOUT_MILLIS) { awaitFix(manager, provider) }
            if (fix != null) return fix.withName()
        }
        return lastKnown(manager)?.withName()
    }

    /**
     * The platform Geocoder asks the system's own location service for a city name, so the app
     * itself still talks only to Open-Meteo. No backend, or no answer, leaves the name empty.
     */
    private suspend fun LatLon.withName(): LatLon {
        val name = withTimeoutOrNull(GEOCODE_TIMEOUT_MILLIS) { cityName(latitude, longitude) }
        return if (name.isNullOrBlank()) this else copy(name = name)
    }

    @Suppress("DEPRECATION")
    private suspend fun cityName(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.SIMPLIFIED_CHINESE)
        val addresses: List<Address>? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) continuation.resume(addresses)
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        },
                    )
                }
            } else {
                withContext(Dispatchers.IO) { geocoder.getFromLocation(latitude, longitude, 1) }
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        val address = addresses?.firstOrNull() ?: return null
        return listOf(address.locality, address.subAdminArea, address.adminArea)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
    }

    private fun coarseProviders(manager: LocationManager): List<String> {
        val names = buildList {
            add(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        }
        return names.filter { providerEnabled(manager, it) }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(manager: LocationManager): LatLon? {
        val names = buildList {
            add(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        val newest = names.mapNotNull { name ->
            try {
                manager.getLastKnownLocation(name)
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }.maxByOrNull { it.time }
        return newest.toLatLon()
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
