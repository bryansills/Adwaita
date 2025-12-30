package ninja.bryansills.adwaita

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.toLocalDateTime
import ninja.bryansills.adwaita.databinding.ItemHourBinding
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
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
        val offsetInstant = Clock.System.now() + weather.offset_hour.hours
        val offsetTime = offsetInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        binding.hourlyTime.text = HourFormatter.format(offsetTime.time)

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

private val HourFormatter = LocalTime.Format {
    amPmHour(padding = Padding.NONE)
    amPmMarker(am = "AM", pm = "PM")
}