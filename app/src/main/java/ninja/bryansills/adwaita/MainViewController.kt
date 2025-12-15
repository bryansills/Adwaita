package ninja.bryansills.adwaita

import android.location.Location
import android.util.Log
import kotlin.concurrent.thread

class MainViewController(
    private val locationProvider: LocationProvider,
    private val weatherService: WeatherService
) : ViewController {
    val neededPermissions: Array<String>
        get() = locationProvider.neededPermissions

    fun wasGrantedPermission(permissions: Array<out String?>, grantResults: IntArray): Boolean {
        return locationProvider.wasGrantedPermission(permissions, grantResults)
    }

    private var currentUiState: MainUiState = MainUiState.WaitingForPermission

    private val currentListeners = mutableMapOf<String, (MainUiState) -> Unit>()

    private var locationCallback: ((Location) -> Unit)? = null

    fun registerToUiState(tag: String, callback: (MainUiState) -> Unit) {
        currentListeners[tag] = callback

        callback(currentUiState)

        if (locationCallback == null) {
            val newLocationCallback = { newLocation: Location? ->
                if (newLocation != null) {
                    val latitude = newLocation.latitude.toInt()
                    val longitude = newLocation.longitude.toInt()

                    currentUiState = MainUiState.LocationFound(
                        latitude = latitude,
                        longitude = longitude
                    )
                    currentListeners.values.forEach { listener ->
                        listener(currentUiState)
                    }

                    weatherService.getForecast(
                        latitude = latitude,
                        longitude = longitude
                    ) { response ->
                        Log.d("BLARG", "Hey the ViewController has the response: $response")
                    }
                } else {
                    currentUiState = MainUiState.CannotGetLocation
                    currentListeners.values.forEach { listener ->
                        listener(currentUiState)
                    }
                }
            }
            locationProvider.getCurrentLocation(TAG, newLocationCallback)
            locationCallback = newLocationCallback
        }
    }

    fun unregisterToUiState(tag: String) {
        currentListeners.remove(tag)

        if (currentListeners.isEmpty() && locationCallback != null) {
            thread {
                Thread.sleep(5_000)

                // double check that there are still no listeners
                if (currentListeners.isEmpty() && locationCallback != null) {
                    locationProvider.cancelRequest(TAG)
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
            locationProvider.cancelRequest(TAG)
            locationCallback = null
        }
    }

    companion object {
        private val TAG = MainViewController::class.java.name
    }
}

sealed interface MainUiState {
    data object WaitingForPermission : MainUiState

    data object CannotGetLocation : MainUiState

    data class LocationFound(val latitude: Int, val longitude: Int) : MainUiState
}