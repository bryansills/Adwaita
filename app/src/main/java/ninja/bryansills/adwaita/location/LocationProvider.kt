package ninja.bryansills.adwaita.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v4.content.PermissionChecker
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED
import android.util.Log

interface LocationProvider {

    val hasPermission: Boolean
    fun addListener(listener: Listener)

    fun removeListener(listener: Listener)

    interface Listener {
        fun onLocationChanged(location: Location)
    }
}

class DefaultLocationProvider(
    private val locationManager: LocationManager,
    private val context: Context
) : LocationProvider {
    private val currentListeners = mutableListOf<LocationProvider.Listener>()

    private var internalListener: LocationListener? = null
    private var lastLocation: Location? = null

    override val hasPermission: Boolean
        get() {
            val result = PermissionChecker.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            return result == PERMISSION_GRANTED
        }

    override fun addListener(listener: LocationProvider.Listener) {
//        if (!hasPermission) {
//            return // early exit!
//        }

        // if we already have the location, send it
        lastLocation?.let { listener.onLocationChanged(it) }

        // start listening to the system if we aren't already
        if (internalListener == null) {
            val newInternalListener = LocationListener { newLocation ->
                currentListeners.forEach { it.onLocationChanged(newLocation) }
            }
            val providerOptions = locationManager.allProviders
            val bestProvider = locationManager.bestProvider
            Log.d("BLARG", providerOptions.joinToString())

            if (bestProvider != null) {
//                locationManager.requestLocationUpdates(
//                    bestProvider,
//                    DurationBetweenUpdatesInMillis,
//                    DistanceBetweenUpdatesInMeters,
//                    newInternalListener
//                )
            } else {
                Log.w("BLARG", "Cannot get location updates because device has no location providers")
            }

            internalListener = newInternalListener
        }
    }

    override fun removeListener(listener: LocationProvider.Listener) {
        if (!hasPermission) {
            return // early exit!
        }

        currentListeners.remove(listener)

        if (currentListeners.isEmpty()) {
            internalListener?.let {
                locationManager.removeUpdates(it)
                internalListener = null
            }
        }
    }

    companion object {
        private val DurationBetweenUpdatesInMillis = 30_000L
        private val DistanceBetweenUpdatesInMeters = 1_000f
    }
}

private val LocationManager.bestProvider: String?
    get() {
        val allOptions = this.getProviders(true)
        return allOptions.find { it == LocationManager.FUSED_PROVIDER }
            ?: allOptions.find { it == LocationManager.NETWORK_PROVIDER }
            ?: allOptions.find { it == LocationManager.GPS_PROVIDER }
            ?: run {
                val firstOption = allOptions.firstOrNull()
                Log.w("BLARG", "Trying to get location updates, but the best provider is $firstOption")
                firstOption
            }
    }