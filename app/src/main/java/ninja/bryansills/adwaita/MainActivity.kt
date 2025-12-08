package ninja.bryansills.adwaita

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import ninja.bryansills.adwaita.location.LocationProvider

class MainActivity : AppCompatActivity() {

    private var _locationProvider: LocationProvider? = null
    private val locationProvider: LocationProvider
        get() = checkNotNull(_locationProvider) {
            "You're trying to get location data, but the UI isn't visible..."
        }

    private var locationListener: LocationProvider.Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _locationProvider = (this.application as AdwaitaApplication).locationProvider

        if (!locationProvider.hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                locationProvider.requiredPermissions,
                LocationRequestCode
            )
        } else {
            listenToLocation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationListener?.let { locationProvider.removeListener(it) }
        locationListener = null
        _locationProvider = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            LocationRequestCode -> {
                if (locationProvider.wasGrantedPermission(permissions, grantResults)) {
                    listenToLocation()
                } else {
                    Log.w("BLARG", "User doesn't want us to see their location")
                }
            }
            else -> {
                Log.w("BLARG", "Unknown request for permissions $requestCode")
            }
        }
    }

    private fun listenToLocation() {
        val newListener = object : LocationProvider.Listener {
            override fun onLocationChanged(location: Location) {
                val mainText = findViewById(R.id.hello_world) as TextView
                mainText.text = "${location.latitude}, ${location.longitude}"
            }
        }
        locationProvider.addListener(newListener)
        locationListener = newListener
    }

    companion object {
        private const val LocationRequestCode = 123567
    }
}