package ninja.bryansills.adwaita

import android.os.Handler
import android.os.Looper
import androidx.core.os.ExecutorCompat
import java.util.concurrent.Executors

interface ViewController {
    fun onCleared() {}
}

class ViewControllerStore(private val factory: ViewControllerFactory) {
    private val currentViewControllers: MutableMap<Class<*>, ViewController> = mutableMapOf()

    fun <VC : ViewController> get(clazz: Class<VC>): VC {
        return currentViewControllers.getOrPut(clazz) {
            factory.create(clazz)
        } as VC
    }

    fun <VC : ViewController> remove(clazz: Class<VC>) {
        currentViewControllers.remove(clazz)
    }
}

inline fun <reified VC : ViewController> ViewControllerStore.get(): VC {
    return this.get(VC::class.java)
}

class ViewControllerFactory(
    private val locationProvider: LocationProvider,
    private val weatherService: WeatherService
) {
    fun <VC : ViewController> create(clazz: Class<VC>): VC {
        return when (clazz) {
            MainViewController::class.java -> {
                MainViewController(
                    locationProvider = locationProvider,
                    weatherService = weatherService,
                    backgroundExecutor = Executors.newCachedThreadPool(),
                    mainThreadExecutor = ExecutorCompat.create(Handler(Looper.getMainLooper()))
                )
            }
            else -> throw IllegalArgumentException("Cannot create ViewController for ${clazz.name}")
        } as VC
    }
}