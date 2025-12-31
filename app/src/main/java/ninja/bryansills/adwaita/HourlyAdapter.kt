package ninja.bryansills.adwaita

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ninja.bryansills.adwaita.databinding.ItemHourBinding
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.ExperimentalTime

class HourlyAdapter(private val hours: List<HourlyWeather>) : RecyclerView.Adapter<HourlyViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HourlyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemHourBinding.inflate(inflater, parent, false)
        return HourlyViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: HourlyViewHolder,
        position: Int
    ) {
        holder.bind(hours[position])
    }

    override fun getItemCount(): Int {
        return hours.size
    }
}

class HourlyViewHolder(private val binding: ItemHourBinding) : RecyclerView.ViewHolder(binding.root) {
    @OptIn(ExperimentalTime::class)
    fun bind(weather: HourlyWeather) {
        val offsetInst = Instant.now() + Duration.ofHours(weather.offset_hour.toLong())
        val offsetLocalTime = LocalTime.ofInstant(offsetInst, ZoneId.systemDefault())
        binding.hourlyTime.text = LocalTimeFormatter.format(offsetLocalTime)

        binding.hourlyStatus.text = weather.code

        if (weather.precipitation_chance > 0) {
            binding.hourlyPercentPrecipitation.visibility = View.VISIBLE
            binding.hourlyPercentPrecipitation.text = binding.root.context.getString(R.string.percent_precipitation, weather.precipitation_chance)
        } else {
            binding.hourlyPercentPrecipitation.visibility = View.GONE
        }

        binding.hourlyTemperature.text = binding.root.context.getString(R.string.temperature, weather.temp)
    }
}

private val LocalTimeFormatter = DateTimeFormatter.ofPattern("ha", Locale.getDefault())
