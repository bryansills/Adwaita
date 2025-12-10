package ninja.bryansills.adwaita

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

    val neededPermissions: Array<String>

    fun wasGrantedPermission(permissions: Array<out String?>, grantResults: IntArray): Boolean

    fun addListener(tag: String, callback: (Location) -> Unit)

    fun removeListener(tag: String)
}

class DefaultLocationProvider(
    private val locationManager: LocationManager,
    private val context: Context
) : LocationProvider {
    private val currentListeners = mutableMapOf<String, (Location) -> Unit>()

    private var internalListener: LocationListener? = null
    private var lastLocation: Location? = null

    private val hasPermission: Boolean
        get() {
            val result = PermissionChecker.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            return result == PERMISSION_GRANTED
        }

    override val neededPermissions: Array<String>
        get() {
            return if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                arrayOf()
            }
        }

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

    override fun addListener(tag: String, callback: (Location) -> Unit) {
        if (!hasPermission) {
            return // early exit!
        }

        currentListeners[tag] = callback

        // if we already have the location, send it
        lastLocation?.let { callback(it) }

        // start listening to the system if we aren't already
        if (internalListener == null) {
            val newInternalListener = LocationListener { newLocation ->
                currentListeners.values.forEach { listener ->
                    listener(newLocation)
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

    override fun removeListener(tag: String) {
        if (!hasPermission) {
            return // early exit!
        }

        currentListeners.remove(tag)

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