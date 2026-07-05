package com.lanxin.prophet.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.lanxin.prophet.model.LocationSnapshot
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine

class DeviceLocationRepository(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocationRepository {
    override suspend fun getCurrentLocationOrNull(): LocationSnapshot? = withContext(dispatcher) {
        if (!hasLocationPermission()) {
            return@withContext null
        }

        val manager = context.getSystemService(LocationManager::class.java) ?: return@withContext null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestCurrentLocation(manager)
        } else {
            val provider = pickProvider(manager) ?: return@withContext null
            manager.getLastKnownLocation(provider)?.toSnapshot()
        }
    }

    private suspend fun requestCurrentLocation(manager: LocationManager): LocationSnapshot? {
        val provider = pickProvider(manager) ?: return null
        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }

            manager.getCurrentLocation(
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(context)
            ) { location ->
                continuation.resume(location?.toSnapshot())
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun pickProvider(manager: LocationManager): String? {
        val preferredProviders = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        return preferredProviders.firstOrNull { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        } ?: preferredProviders.firstOrNull { provider ->
            runCatching { manager.getProvider(provider) != null }.getOrDefault(false)
        }
    }
}

private fun Location.toSnapshot(): LocationSnapshot {
    return LocationSnapshot(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        provider = provider.orEmpty()
    )
}
