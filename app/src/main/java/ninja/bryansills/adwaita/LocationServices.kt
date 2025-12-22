package ninja.bryansills.adwaita

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderApi
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import com.google.android.gms.location.LocationServices as GoogleLocationServices

interface LocationServices : LocationPermissions {
    fun getLastLocation(cancellationSignal: CancellationSignal, callback: (Location?) -> Unit)
}

class PlayServicesLocationServices(
    context: Context,
    private val executor: Executor,
    private val locationPermissions: LocationPermissions
) : LocationServices, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private val googleApiClient: GoogleApiClient = GoogleApiClient
        .Builder(context)
        .addApi(GoogleLocationServices.API)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build()

    init {
        googleApiClient.connect()
    }

    private var internalConnectionResult: ConnectionResult? = null

    private val isConnected: Boolean
        get() = internalConnectionResult == CONNECTED_RESULT

    override fun getLastLocation(
        cancellationSignal: CancellationSignal,
        callback: (Location?) -> Unit
    ) {
        executor.execute {
            var playServicesConnectionCount = 0

            Log.d("BLARG", "Cancel sig is canceled ${cancellationSignal.isCanceled}")
            Log.d("BLARG", "This is connected ${this.isConnected}")
            Log.d("BLARG", "Has timed out ${hasTimedOut(playServicesConnectionCount)}")
            while (!cancellationSignal.isCanceled && !this.isConnected && !hasTimedOut(playServicesConnectionCount)) {
                Log.d("BLARG", "Polling attempt $playServicesConnectionCount")
                Thread.sleep(PLAY_SERVICES_CONNECTION_POLL_RATE_MILLIS)
                playServicesConnectionCount++
            }

            if (hasTimedOut(playServicesConnectionCount)) {
                callback(null)
                return@execute
            }

            if (cancellationSignal.isCanceled) {
                return@execute
            }

            val lastLocation = GoogleLocationServices.FusedLocationApi.getLastLocation(googleApiClient)
            if (lastLocation != null && lastLocation.isFresh) {
                callback(lastLocation)
                return@execute
            }

            val cancelableGoogleLocationListener = CancelableGoogleLocationListener(
                executor = executor,
                fusedLocationProviderApi = GoogleLocationServices.FusedLocationApi,
                googleApiClient = googleApiClient,
                callback = callback
            )
            GoogleLocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient,
                LocationRequest.create().setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY),
                cancelableGoogleLocationListener
            )

            cancellationSignal.setOnCancelListener(cancelableGoogleLocationListener::cancel)
            cancelableGoogleLocationListener.startTimeout(GET_LOCATION_TIMEOUT_MILLIS)
        }
    }

    override fun onConnected(p0: Bundle?) {
        Log.d("BLARG", "GPS connected")
        internalConnectionResult = CONNECTED_RESULT
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d("BLARG", "GPS suspended $p0")
        internalConnectionResult = ConnectionResult(ConnectionResult.API_UNAVAILABLE)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.d("BLARG", "GPS failed ${p0.errorMessage}")
        internalConnectionResult = p0
    }

    override val hasPermission: Boolean
        get() = locationPermissions.hasPermission

    override val neededPermissions: Array<String>
        get() = locationPermissions.neededPermissions

    override fun wasGrantedPermission(
        permissions: Array<out String?>,
        grantResults: IntArray
    ): Boolean {
        return locationPermissions.wasGrantedPermission(permissions, grantResults)
    }

    private fun hasTimedOut(playServicesConnectionCount: Int): Boolean {
        val alreadyWaited = playServicesConnectionCount * PLAY_SERVICES_CONNECTION_POLL_RATE_MILLIS
        return alreadyWaited > PLAY_SERVICES_CONNECTION_TIMEOUT_MILLIS
    }

    companion object {
        private const val PLAY_SERVICES_CONNECTION_TIMEOUT_MILLIS = 15_000L
        private const val GET_LOCATION_TIMEOUT_MILLIS = 15_000L
        private const val PLAY_SERVICES_CONNECTION_POLL_RATE_MILLIS = 250L

        private val CONNECTED_RESULT = ConnectionResult(ConnectionResult.SUCCESS)
    }

    private class CancelableGoogleLocationListener(
        private val executor: Executor,
        private val fusedLocationProviderApi: FusedLocationProviderApi,
        private val googleApiClient: GoogleApiClient,
        callback: (Location?) -> Unit
    ) : LocationListener {
        private var triggered = false
        private var internalCallback: ((Location?) -> Unit)? = callback
        private var timeoutRunnable: Runnable? = null
        private val timeoutHandler = Handler(Looper.getMainLooper())

        override fun onLocationChanged(p0: Location?) {
            synchronized(this) {
                if (triggered) {
                    return
                }
                triggered = true
            }

            val callback = internalCallback
            executor.execute { callback?.invoke(p0) }

            cleanup()
        }

        fun startTimeout(timeoutMillis: Long) {
            synchronized(this) {
                if (triggered) {
                    return
                }

                val onTimeout = Runnable {
                    timeoutRunnable = null
                    onLocationChanged(null)
                }
                timeoutRunnable = onTimeout
                timeoutHandler.postDelayed(onTimeout, timeoutMillis)
            }
        }

        fun cancel() {
            synchronized(this) {
                if (triggered) {
                    return
                }
                triggered = true
            }

            cleanup()
        }

        private fun cleanup() {
            internalCallback = null
            fusedLocationProviderApi.removeLocationUpdates(googleApiClient, this)

            timeoutRunnable?.let {
                timeoutHandler.removeCallbacks(it)
                timeoutRunnable = null
            }
        }
    }
}

class AospLocationServices(
    private val locationManager: LocationManager,
    private val executor: Executor,
    private val locationPermissions: LocationPermissions
) : LocationServices {
    private var lastLocation: Location? = null

    override fun getLastLocation(
        cancellationSignal: CancellationSignal,
        callback: (Location?) -> Unit
    ) {
        if (!hasPermission) {
            return // early exit!
        }

        // if we already have a fresh location, send it
        lastLocation?.takeIf { it.isFresh }?.let {
            callback(it)
            return // early exit!
        }

        val bestProviders = locationManager.bestProviders

        if (bestProviders.isEmpty()) {
            Log.w("BLARG", "Cannot get location updates because device has no good location providers")
            return // early exit!
        }

        val internalCancellationSignals = mutableListOf<CancellationSignal>()
        cancellationSignal.setOnCancelListener {
            internalCancellationSignals.forEach { it.cancel() }
        }
        var internalCancellationCount = 0

        val internalConsumer = Consumer<Location?> { newLocation ->
            Log.d("BLARG", "Got a new location $newLocation")
            if (newLocation != null) {
                callback(newLocation)
                internalCancellationSignals.forEach { it.cancel() }
            } else {
                internalCancellationCount++
                Log.d("BLARG", "Null emitted $internalCancellationCount times")

                if (internalCancellationCount == bestProviders.size) {
                    Log.d("BLARG", "Giving up")
                    callback(null)
                }
            }
        }

        bestProviders.forEach { provider ->
            val providerCancellationSignal = CancellationSignal()
            internalCancellationSignals.add(providerCancellationSignal)

            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                providerCancellationSignal,
                executor,
                internalConsumer
            )
        }
    }

    override val hasPermission: Boolean
        get() = locationPermissions.hasPermission

    override val neededPermissions: Array<String>
        get() = locationPermissions.neededPermissions

    override fun wasGrantedPermission(
        permissions: Array<out String?>,
        grantResults: IntArray
    ): Boolean {
        return locationPermissions.wasGrantedPermission(permissions, grantResults)
    }
}

private val LocationManager.bestProviders: List<String>
    get() {
        val allOptions = this.getProviders(true)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && allOptions.contains(LocationManager.FUSED_PROVIDER)) {
            listOf(LocationManager.FUSED_PROVIDER)
        } else {
            allOptions
        }
    }

private const val MAX_CURRENT_LOCATION_AGE_IN_MILLIS: Long = 15 * 60 * 1000

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