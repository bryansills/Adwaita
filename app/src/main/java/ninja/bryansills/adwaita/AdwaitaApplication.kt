package ninja.bryansills.adwaita

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.multidex.MultiDex
import ninja.bryansills.adwaita.location.DefaultLocationProvider

class AdwaitaApplication : Application() {
    lateinit var viewControllerStore: ViewControllerStore

    override fun onCreate() {
        super.onCreate()

        val locationProvider = DefaultLocationProvider(
            this.getSystemService(LOCATION_SERVICE) as LocationManager,
            this
        )
        val viewControllerFactory = ViewControllerFactory(locationProvider)
        viewControllerStore = ViewControllerStore(viewControllerFactory)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
