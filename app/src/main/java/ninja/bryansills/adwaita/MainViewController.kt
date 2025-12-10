package ninja.bryansills.adwaita

import android.location.Location
import android.util.Log
import ninja.bryansills.adwaita.location.LocationProvider
import kotlin.concurrent.thread

class MainViewController(private val locationProvider: LocationProvider) : ViewController {
    val neededPermissions: Array<String>
        get() = locationProvider.neededPermissions

    fun wasGrantedPermission(permissions: Array<out String?>, grantResults: IntArray): Boolean {
        return locationProvider.wasGrantedPermission(permissions, grantResults)
    }

    private var currentUiState: MainUiState = MainUiState.WaitingForPermission

    private val currentListeners = mutableMapOf<String, (MainUiState) -> Unit>()

    private var locationCallback: ((Location) -> Unit)? = null

    fun listenToUiState(tag: String, callback: (MainUiState) -> Unit) {
        currentListeners[tag] = callback

        callback(currentUiState)

        if (locationCallback == null) {
            val newLocationCallback = { newLocation: Location ->
                currentUiState = MainUiState.LocationFound(
                    latitude = newLocation.latitude.toInt(),
                    longitude = newLocation.longitude.toInt()
                )
                currentListeners.values.forEach { listener ->
                    listener(currentUiState)
                }
            }
            locationProvider.addListener(tag, newLocationCallback)
            locationCallback = newLocationCallback
        }
    }

    fun stopListening(tag: String) {
        currentListeners.remove(tag)

        if (currentListeners.isEmpty() && locationCallback != null) {
            thread {
                Thread.sleep(5_000)

                // double check that there are still no listeners
                if (currentListeners.isEmpty() && locationCallback != null) {
                    locationProvider.removeListener(tag)
                    locationCallback = null
                } else {
                    Log.d("BLARG", "I guess someone started listening again before the 5 second timeout")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("BLARG", "Clearing MainViewController")

        currentListeners.clear()

        if (locationCallback != null) {
            locationProvider.removeListener(TAG)
            locationCallback = null
        }
    }

    companion object {
        private val TAG = MainViewController::class.java.name
    }
}

sealed interface MainUiState {
    data object WaitingForPermission : MainUiState

    data class LocationFound(val latitude: Int, val longitude: Int) : MainUiState
}