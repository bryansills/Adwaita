package ninja.bryansills.adwaita

import android.app.Application
import android.content.Context
import android.location.LocationManager
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
        val viewControllerFactory = ViewControllerFactory(
            locationProvider = DefaultLocationProvider(
                this.getSystemService(LOCATION_SERVICE) as LocationManager,
                this,
                Executors.newSingleThreadExecutor()
            ),
            weatherService = DefaultWeatherService(
                okHttpClient = OkHttpClient(),
                gson = Gson(),
                executor = backgroundExecutor
            ),
            backgroundExecutor = backgroundExecutor,
            mainThreadExecutor = ExecutorCompat.create(Handler(Looper.getMainLooper()))
        )
        viewControllerStore = ViewControllerStore(viewControllerFactory)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
