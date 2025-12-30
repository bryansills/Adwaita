package ninja.bryansills.adwaita

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import ninja.bryansills.adwaita.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var _mainViewController: MainViewController? = null
    private val mainViewController: MainViewController
        get() = checkNotNull(_mainViewController) {
            "You can only access the ViewController when the lifecycle state is CREATED or higher"
        }

    private var uiStateListener: ((MainUiState) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        _mainViewController = (this.application as AdwaitaApplication).viewControllerStore.get()

        if (mainViewController.neededPermissions.isNotEmpty()) {
            requestPermissions(
                mainViewController.neededPermissions,
                PERMISSION_REQUEST_CODE
            )
        } else {
            mainViewController.requestForecast()
        }

        val newUiStateListener = { uiState: MainUiState ->
            updateUi(uiState)
        }
        mainViewController.registerToUiState(TAG, newUiStateListener)
        uiStateListener = newUiStateListener
    }

    override fun onDestroy() {
        super.onDestroy()

        mainViewController.unregisterToUiState(TAG)
        uiStateListener = null

        if (isFinishing) {
            (this.application as AdwaitaApplication).viewControllerStore.remove(MainViewController::class.java)
        }
        _mainViewController = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (mainViewController.wasGrantedPermission(permissions, grantResults)) {
                    mainViewController.requestForecast()
                } else {
                    Log.w("BLARG", "User doesn't want us to see their location")
                }
            }
            else -> {
                Log.w("BLARG", "Unknown request for permissions $requestCode")
            }
        }
    }

    private fun updateUi(uiState: MainUiState) {
        when (uiState) {
            MainUiState.WaitingForPermission -> {
                binding.errorView.visibility = View.GONE
                binding.forecastView.root.visibility = View.GONE

                binding.inProgressView.visibility = View.VISIBLE
                binding.inProgressText.text = getString(R.string.we_need_permission)

            }
            MainUiState.RequestingLocation -> {
                binding.errorView.visibility = View.GONE
                binding.forecastView.root.visibility = View.GONE

                binding.inProgressView.visibility = View.VISIBLE
                binding.inProgressText.text = getString(R.string.trying_to_locate)
            }
            is MainUiState.LocationFound -> {
                binding.errorView.visibility = View.GONE
                binding.forecastView.root.visibility = View.GONE

                binding.inProgressView.visibility = View.VISIBLE
                binding.inProgressText.text = getString(R.string.getting_the_latest)
            }
            MainUiState.CannotGetLocation -> {
                binding.inProgressView.visibility = View.GONE
                binding.forecastView.root.visibility = View.GONE

                binding.errorView.visibility = View.VISIBLE
                binding.errorText.text = getString(R.string.cannot_get_location)
            }
            is MainUiState.NetworkRequestFailed -> {
                binding.inProgressView.visibility = View.GONE
                binding.forecastView.root.visibility = View.GONE

                binding.errorView.visibility = View.VISIBLE
                binding.errorText.text = getString(R.string.error_getting, uiState.exception.message)
            }
            is MainUiState.ForecastFound -> {
                binding.errorView.visibility = View.GONE
                binding.inProgressView.visibility = View.GONE

                binding.forecastView.apply {
                    root.visibility = View.VISIBLE
                    currentView.currentTemperature.text = getString(R.string.temperature, uiState.forecast.current.temp)
                    currentView.feelsLike.text = getString(R.string.feels_like_temperature, uiState.forecast.current.feels_like)
                    currentView.dailyHigh.text = getString(R.string.daily_high_temperature, uiState.forecast.current.daily_high)
                    currentView.dailyLow.text = getString(R.string.daily_low_temperature, uiState.forecast.current.daily_low)
                    hourlyForecast.adapter = HourlyAdapter(uiState.forecast.hourly)
                }
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.name
        private const val PERMISSION_REQUEST_CODE = 123567
    }
}