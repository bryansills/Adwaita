package ninja.bryansills.adwaita

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.multidex.MultiDex
import ninja.bryansills.adwaita.location.DefaultLocationProvider
import ninja.bryansills.adwaita.location.LocationProvider

class AdwaitaApplication : Application() {

    lateinit var locationProvider: LocationProvider

    override fun onCreate() {
        super.onCreate()

        locationProvider = DefaultLocationProvider(
            this.getSystemService(LOCATION_SERVICE) as LocationManager,
            this
        )
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
