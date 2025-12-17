package ninja.bryansills.adwaita

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.squareup.okhttp.OkHttpClient
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.Executor

interface WeatherService {
    fun getForecast(latitude: Int, longitude: Int, callback: (Result<WeatherResponse>) -> Unit)
}

class DefaultWeatherService(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val executor: Executor
) : WeatherService {
    override fun getForecast(
        latitude: Int,
        longitude: Int,
        callback: (Result<WeatherResponse>) -> Unit
    ) {
        executor.execute {
            var inputStream: InputStream? = null
            var inputStreamReader: InputStreamReader? = null

            try {
                val connection = okHttpClient.open(formatUrl(latitude, longitude))
                inputStream = connection.getInputStream()
                inputStreamReader = InputStreamReader(inputStream)
                val response = gson.fromJson(inputStreamReader, WeatherResponse::class.java)
                Log.d("BLARG", "Successful network request: $response")
                callback(Result.success(response))
            } catch (ex: IOException) {
                Log.w("BLARG", "Network request failed: ${ex.message}")
                callback(Result.failure(ex))
            } catch (ex: JsonParseException) {
                Log.w("BLARG", "JSON parsing failed: ${ex.message}")
                callback(Result.failure(ex))
            } finally {
                inputStream?.close()
                inputStreamReader?.close()
            }
        }
    }

    private fun formatUrl(latitude: Int, longitude: Int): URL {
        return URL("http://bryansills.ninja/Adwaita/data/${latitude}_$longitude.json")
    }
}