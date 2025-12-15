package ninja.bryansills.adwaita

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.support.v4.content.PermissionChecker
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED
import android.util.Log
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

interface LocationProvider {

    val neededPermissions: Array<String>

    fun wasGrantedPermission(permissions: Array<out String?>, grantResults: IntArray): Boolean

    fun getCurrentLocation(tag: String, callback: (Location?) -> Unit)

    fun cancelRequest(tag: String)
}

class DefaultLocationProvider(
    private val locationManager: LocationManager,
    private val context: Context,
    private val executor: Executor
) : LocationProvider {
    private val currentListeners = mutableMapOf<String, (Location?) -> Unit>()

    private var internalCancellationSignal: CancellationSignal? = null
    private var internalLocationConsumer: Consumer<Location?>? = null
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

    override fun getCurrentLocation(tag: String, callback: (Location?) -> Unit) {
        if (!hasPermission) {
            return // early exit!
        }

        // if we already have a fresh location, send it
        lastLocation?.takeIf { it.isFresh }?.let {
            callback(it)
            return // early exit!
        }

        currentListeners[tag] = callback

        // start listening to the system if we aren't already
        if (internalLocationConsumer == null) {
            val bestProvider = locationManager.bestProvider

            if (bestProvider != null) {
                val newCancellationSignal = CancellationSignal()
                val newLocationConsumer = Consumer<Location?> { newLocation ->
                    currentListeners.values.forEach { listener ->
                        listener(newLocation)
                    }

                    lastLocation = newLocation

                    currentListeners.clear()
                    internalCancellationSignal = null
                    internalLocationConsumer = null
                }

                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    bestProvider,
                    newCancellationSignal,
                    executor,
                    newLocationConsumer
                )

                internalCancellationSignal = newCancellationSignal
                internalLocationConsumer = newLocationConsumer
            } else {
                Log.w("BLARG", "Cannot get location updates because device has no good location providers")
            }
        }
    }

    override fun cancelRequest(tag: String) {
        if (!hasPermission) {
            return // early exit!
        }

        currentListeners.remove(tag)

        if (currentListeners.isEmpty() && internalCancellationSignal?.isCanceled == true) {
            internalCancellationSignal?.cancel()
            internalCancellationSignal = null
            internalLocationConsumer = null
        }
    }
}

private const val MAX_CURRENT_LOCATION_AGE_IN_MILLIS: Long = 30 * 1000

private val Location.isFresh: Boolean
    get() {
        val locationAge = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val currentTime = SystemClock.elapsedRealtime()
            val locationTimestamp = TimeUnit.NANOSECONDS.toMillis(this.elapsedRealtimeNanos)

            currentTime - locationTimestamp
        } else {
            val currentTime = System.currentTimeMillis()
            val locationTimestamp = this.time
            (currentTime - locationTimestamp)
        }

        return locationAge < MAX_CURRENT_LOCATION_AGE_IN_MILLIS
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