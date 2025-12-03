package ninja.bryansills.adwaita

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mainText = findViewById(R.id.hello_world) as TextView
        mainText.text = DoesThisWork().showThis
    }
}