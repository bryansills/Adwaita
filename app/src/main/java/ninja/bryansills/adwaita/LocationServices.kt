package ninja.bryansills.adwaita

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.core.os.CancellationSignal
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import java.util.concurrent.Executor
import com.google.android.gms.location.LocationServices as GoogleLocationServices

interface LocationServices {
    val isConnected: Boolean

    fun getLastLocation(cancellationSignal: CancellationSignal, callback: (Location?) -> Unit)
}

class DefaultLocationServices(
    private val context: Context,
    private val executor: Executor
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

    override val isConnected: Boolean
        get() = internalConnectionResult == CONNECTED_RESULT

    override fun getLastLocation(
        cancellationSignal: CancellationSignal,
        callback: (Location?) -> Unit
    ) {
        executor.execute {
            var playServicesConnectionCount = 0

            while (!cancellationSignal.isCanceled && !this.isConnected && playServicesConnectionCount < 120) {
                Thread.sleep(250)
                playServicesConnectionCount++
            }

            val lastLocation = GoogleLocationServices.FusedLocationApi.getLastLocation(googleApiClient)
            if (lastLocation != null) {
                callback(lastLocation)
                return@execute
            }
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

    companion object {
        private val CONNECTED_RESULT = ConnectionResult(ConnectionResult.SUCCESS)
    }
}