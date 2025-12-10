package ninja.bryansills.adwaita

data class WeatherResponse(
    val current: CurrentWeather,
    val hourly: List<HourlyWeather>
)

data class CurrentWeather(
    val temp: Int,
    val feels_like: Int,
    val daily_high: Int,
    val daily_low: Int,
    val code: String
)

data class HourlyWeather(
    val offset_hour: Int,
    val temp: Int,
    val precipitation_chance: Int,
    val code: String
)