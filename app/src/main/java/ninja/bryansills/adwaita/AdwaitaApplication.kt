package ninja.bryansills.adwaita

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.os.ExecutorCompat
import androidx.multidex.MultiDex
import com.google.gson.Gson
import com.squareup.okhttp.OkHttpClient
import java.util.concurrent.Executors

class AdwaitaApplication : Application() {
    lateinit var viewControllerStore: ViewControllerStore

    override fun onCreate() {
        super.onCreate()

        val backgroundExecutor = Executors.newCachedThreadPool()
        val mainThreadExecutor = ExecutorCompat.create(Handler(Looper.getMainLooper()))

        val locationPermissions: LocationPermissions = DefaultLocationPermissions(this)
        val locationServices: LocationServices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            PlayServicesLocationServices(this, mainThreadExecutor, locationPermissions)
        } else {
            AospLocationServices(
                locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager,
                executor = mainThreadExecutor,
                locationPermissions = locationPermissions
            )
        }

        val viewControllerFactory = ViewControllerFactory(
            locationServices = locationServices,
            weatherService = DefaultWeatherService(
                okHttpClient = OkHttpClient(),
                gson = Gson(),
                executor = backgroundExecutor
            ),
            backgroundExecutor = backgroundExecutor,
            mainThreadExecutor = mainThreadExecutor
        )
        viewControllerStore = ViewControllerStore(viewControllerFactory)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
