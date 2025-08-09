package it.serielsrl.batterylog.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import it.serielsrl.batterylog.R
import it.serielsrl.batterylog.data.BatteryEntry
import it.serielsrl.batterylog.databinding.ItemBatteryEntryBinding
import java.util.Locale

class BatteryAdapter : ListAdapter<BatteryEntry, BatteryAdapter.BatteryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatteryViewHolder {
        val binding = ItemBatteryEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BatteryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BatteryViewHolder, position: Int) {
        val currentItem = getItem(position)
        val previousItem = if (position > 0) getItem(position - 1) else null
        holder.bind(currentItem, previousItem)
    }

    class BatteryViewHolder(private val binding: ItemBatteryEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(currentEntry: BatteryEntry, previousEntry: BatteryEntry?) {
            binding.apply {
                tvDate.text = currentEntry.getFormattedDate()
                tvTotalKm.text = root.context.getString(
                    R.string.total_km,
                    currentEntry.totalKm
                )
                tvKmSinceLast.text = root.context.getString(
                    R.string.km_since_last,
                    currentEntry.kmSinceLast
                )
                tvDaysSinceLast.text = root.context.getString(
                    R.string.days_since_last,
                    currentEntry.daysSinceLast
                )
                tvKmPerDay.text = root.context.getString(
                    R.string.km_per_day,
                    currentEntry.kmPerDay
                )
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BatteryEntry>() {
        override fun areItemsTheSame(oldItem: BatteryEntry, newItem: BatteryEntry) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BatteryEntry, newItem: BatteryEntry) =
            oldItem == newItem
    }
}