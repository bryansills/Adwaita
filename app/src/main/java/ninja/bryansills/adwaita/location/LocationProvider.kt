package ninja.bryansills.adwaita.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.support.v4.content.PermissionChecker
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED
import android.util.Log

interface LocationProvider {

    val hasPermission: Boolean

    val requiredPermissions: Array<String>

    fun wasGrantedPermission(permissions: Array<out String?>, grantResults: IntArray): Boolean

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

    override val requiredPermissions: Array<String> = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)

    override fun wasGrantedPermission(
        permissions: Array<out String?>,
        grantResults: IntArray
    ): Boolean {
        val locationIndex = permissions.indexOf(Manifest.permission.ACCESS_COARSE_LOCATION)

        return if (locationIndex >= 0) {
            val grantResult = grantResults[locationIndex]
            grantResult == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    override fun addListener(listener: LocationProvider.Listener) {
        if (!hasPermission) {
            return // early exit!
        }

        currentListeners.add(listener)

        // if we already have the location, send it
        lastLocation?.let { listener.onLocationChanged(it) }

        // start listening to the system if we aren't already
        if (internalListener == null) {
            val newInternalListener = LocationListener { newLocation ->
                currentListeners.forEach {
                    it.onLocationChanged(newLocation)
                    lastLocation = newLocation
                }
            }

            val bestProvider = locationManager.bestProvider

            if (bestProvider != null) {
                locationManager.requestLocationUpdates(
                    bestProvider,
                    DurationBetweenUpdatesInMillis,
                    DistanceBetweenUpdatesInMeters,
                    newInternalListener
                )
            } else {
                Log.w("BLARG", "Cannot get location updates because device has no good location providers")
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

        val bestOption = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            allOptions.find { it == LocationManager.FUSED_PROVIDER }
        } else {
            null
        }

        return bestOption
            ?: allOptions.find { it == LocationManager.NETWORK_PROVIDER }
            ?: allOptions.find { it == LocationManager.GPS_PROVIDER }
            ?: run {
                val firstOption = allOptions.firstOrNull()
                Log.w("BLARG", "Trying to get location updates, but the best provider is $firstOption")
                firstOption
            }
    }