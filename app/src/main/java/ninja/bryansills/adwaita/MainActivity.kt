package ninja.bryansills.adwaita

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private var _mainViewController: MainViewController? = null
    private val mainViewController: MainViewController
        get() = checkNotNull(_mainViewController) {
            "You can only access the ViewController when the lifecycle state is CREATED or higher"
        }

    private var uiStateListener: ((MainUiState) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _mainViewController = (this.application as AdwaitaApplication).viewControllerStore.get()

        if (mainViewController.neededPermissions.isNotEmpty()) {
            requestPermissions(
                mainViewController.neededPermissions,
                PERMISSION_REQUEST_CODE
            )
        } else {
            listenToUiState()
        }
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
                    listenToUiState()
                } else {
                    Log.w("BLARG", "User doesn't want us to see their location")
                }
            }
            else -> {
                Log.w("BLARG", "Unknown request for permissions $requestCode")
            }
        }
    }

    private fun listenToUiState() {
        val newUiStateListener = { uiState: MainUiState ->
            runOnUiThread {
                updateUi(uiState)
            }
        }
        mainViewController.registerToUiState(TAG, newUiStateListener)
        uiStateListener = newUiStateListener
    }

    private fun updateUi(uiState: MainUiState) {
        val mainText = findViewById(R.id.hello_world) as TextView
        mainText.text = when (uiState) {
            MainUiState.WaitingForPermission -> "still loading"
            MainUiState.CannotGetLocation -> "cannot get location"
            is MainUiState.LocationFound -> {
                "${uiState.latitude}, ${uiState.longitude}"
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.name
        private const val PERMISSION_REQUEST_CODE = 123567
    }
}