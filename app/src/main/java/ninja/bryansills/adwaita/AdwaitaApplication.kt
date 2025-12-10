package ninja.bryansills.adwaita

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.multidex.MultiDex
import com.google.gson.Gson
import com.squareup.okhttp.OkHttpClient

class AdwaitaApplication : Application() {
    lateinit var viewControllerStore: ViewControllerStore

    override fun onCreate() {
        super.onCreate()

        val viewControllerFactory = ViewControllerFactory(
            locationProvider = DefaultLocationProvider(
                this.getSystemService(LOCATION_SERVICE) as LocationManager,
                this
            ),
            weatherService = DefaultWeatherService(
                okHttpClient = OkHttpClient(),
                gson = Gson()
            )
        )
        viewControllerStore = ViewControllerStore(viewControllerFactory)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
