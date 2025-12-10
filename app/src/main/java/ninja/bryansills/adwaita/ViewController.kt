package ninja.bryansills.adwaita

import ninja.bryansills.adwaita.location.LocationProvider

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

class ViewControllerFactory(private val locationProvider: LocationProvider) {
    fun <VC : ViewController> create(clazz: Class<VC>): VC {
        return when (clazz) {
            MainViewController::class.java -> MainViewController(locationProvider)
            else -> throw IllegalArgumentException("Cannot create ViewController for ${clazz.name}")
        } as VC
    }
}