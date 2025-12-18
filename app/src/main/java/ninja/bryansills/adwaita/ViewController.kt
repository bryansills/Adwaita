package ninja.bryansills.adwaita

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService

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
    private val locationServices: LocationServices,
    private val weatherService: WeatherService,
    private val backgroundExecutor: ExecutorService,
    private val mainThreadExecutor: Executor
) {
    fun <VC : ViewController> create(clazz: Class<VC>): VC {
        return when (clazz) {
            MainViewController::class.java -> {
                MainViewController(
                    locationServices = locationServices,
                    weatherService = weatherService,
                    backgroundExecutor = backgroundExecutor,
                    mainThreadExecutor = mainThreadExecutor
                )
            }
            else -> throw IllegalArgumentException("Cannot create ViewController for ${clazz.name}")
        } as VC
    }
}