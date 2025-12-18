package ninja.bryansills.adwaita

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.content.PermissionChecker
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED

interface LocationPermissions {

    val hasPermission: Boolean
    val neededPermissions: Array<String>

    fun wasGrantedPermission(permissions: Array<out String?>, grantResults: IntArray): Boolean
}

class DefaultLocationPermissions(private val context: Context) : LocationPermissions {

    override val hasPermission: Boolean
        get() {
            val result = PermissionChecker.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            return result == PERMISSION_GRANTED
        }

    override val neededPermissions: Array<String>
        get() {
            return if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                arrayOf()
            }
        }

    override fun wasGrantedPermission(
        permissions: Array<out String?>,
        grantResults: IntArray
    ): Boolean {
        val locationIndex = permissions.indexOf(Manifest.permission.ACCESS_COARSE_LOCATION)

        return if (locationIndex >= 0) {
            val grantResult = grantResults[locationIndex]
            grantResult == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }
}