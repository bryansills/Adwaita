package ninja.bryansills.adwaita.location

import android.location.Location

interface LocationProvider {
    fun addListener(listener: Listener)

    interface Listener {
        fun onLocationChanged(location: Location)
    }
}
