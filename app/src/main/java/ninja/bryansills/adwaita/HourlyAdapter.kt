package ninja.bryansills.adwaita

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ninja.bryansills.adwaita.databinding.ItemHourBinding

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
    fun bind(weather: HourlyWeather) {
        binding.hourlyTime.text = "9PM"
        binding.hourlyStatus.text = weather.code

        if (weather.precipitation_chance > 0) {
            binding.hourlyPercentPrecipitation.visibility = View.VISIBLE
            binding.hourlyPercentPrecipitation.text = "10%"
        } else {
            binding.hourlyPercentPrecipitation.visibility = View.GONE
        }

        binding.hourlyTemperature.text = "89°"
    }
}