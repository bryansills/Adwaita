package ninja.bryansills.adwaita

import android.util.Log
import com.google.gson.Gson
import com.squareup.okhttp.OkHttpClient
import java.io.InputStreamReader
import java.net.URL
import kotlin.concurrent.thread

interface WeatherService {
    fun getForecast(latitude: Int, longitude: Int, callback: (WeatherResponse) -> Unit)
}

class DefaultWeatherService(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : WeatherService {
    override fun getForecast(
        latitude: Int,
        longitude: Int,
        callback: (WeatherResponse) -> Unit
    ) {
        // TODO: use executors
        thread {
            val connection = okHttpClient.open(formatUrl(latitude, longitude))
            val inputStreamReader = InputStreamReader(connection.getInputStream())
            val response = gson.fromJson(inputStreamReader, WeatherResponse::class.java)
            Log.d("BLARG", "Successful network request: $response")
            callback(response)
        }
    }

    private fun formatUrl(latitude: Int, longitude: Int): URL {
        return URL("http://bryansills.ninja/Adwaita/data/${latitude}_$longitude.json")
    }
}