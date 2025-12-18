package ninja.bryansills.adwaita

import android.location.Location
import android.util.Log
import androidx.core.os.CancellationSignal
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class MainViewController(
    private val locationServices: LocationServices,
    private val weatherService: WeatherService,
    private val backgroundExecutor: ExecutorService,
    private val mainThreadExecutor: Executor,
) : ViewController {
    val neededPermissions: Array<String>
        get() = locationServices.neededPermissions

    fun wasGrantedPermission(permissions: Array<out String?>, grantResults: IntArray): Boolean {
        return locationServices.wasGrantedPermission(permissions, grantResults)
    }

    private var currentUiState: MainUiState = MainUiState.WaitingForPermission

    private val currentListeners = mutableMapOf<String, (MainUiState) -> Unit>()


    private var forecastWork: Future<*>? = null
    private var cancellationSignal: CancellationSignal? = null

    fun registerToUiState(tag: String, callback: (MainUiState) -> Unit) {
        currentListeners[tag] = callback
        callback(currentUiState)
    }

    fun unregisterToUiState(tag: String) {
        currentListeners.remove(tag)
    }

    fun requestForecast() {
        if (forecastWork == null) {
            forecastWork = backgroundExecutor.submit {
                val newCancellationSignal = CancellationSignal()
                cancellationSignal = newCancellationSignal

                locationServices.getLastLocation(newCancellationSignal) { newLocation: Location? ->
                    if (newLocation != null) {
                        val latitude = newLocation.latitude.toInt()
                        val longitude = newLocation.longitude.toInt()

                        updateUiState {
                            MainUiState.LocationFound(
                                latitude = latitude,
                                longitude = longitude
                            )
                        }

                        if (newCancellationSignal.isCanceled) {
                            Log.d("BLARG", "ViewController got cleared before all the work was done. Canceling before making the network request.")
                            return@getLastLocation
                        }

                        weatherService.getForecast(
                            latitude = latitude,
                            longitude = longitude
                        ) { response ->
                            Log.d("BLARG", "Hey the ViewController has the response: $response")

                            response.fold(
                                onSuccess = { forecast ->
                                    updateUiState { MainUiState.ForecastFound(forecast) }
                                },
                                onFailure = { exception ->
                                    updateUiState { MainUiState.NetworkRequestFailed(exception) }
                                }
                            )
                        }
                    } else {
                        updateUiState { MainUiState.CannotGetLocation }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("BLARG", "Clearing MainViewController")

        currentListeners.clear()

        cancellationSignal?.cancel()
        cancellationSignal = null
        forecastWork = null
    }

    private fun updateUiState(onUpdate: (MainUiState) -> MainUiState) {
        mainThreadExecutor.execute {
            val newUiState = onUpdate(currentUiState)
            currentListeners.values.forEach { listener ->
                listener(newUiState)
            }
            currentUiState = newUiState
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

    data class ForecastFound(val forecast: WeatherResponse) : MainUiState

    data class NetworkRequestFailed(val exception: Throwable) : MainUiState
}