package ninja.bryansills.adwaita

import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import ninja.bryansills.adwaita.location.LocationProvider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mainText = findViewById(R.id.hello_world) as TextView
        mainText.text = DoesThisWork().showThis

        val locationProvider = (this.application as AdwaitaApplication).locationProvider
        locationProvider.addListener(object : LocationProvider.Listener {
            override fun onLocationChanged(location: Location) {
//                TODO("Not yet implemented")
            }
        })
    }
}