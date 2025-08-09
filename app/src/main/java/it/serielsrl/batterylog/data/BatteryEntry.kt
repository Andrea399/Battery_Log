package it.serielsrl.batterylog.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Entity(tableName = "battery_entries")
data class BatteryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long, // Stored as timestamp
    val totalKm: Double,
    val kmSinceLast: Double? = null,
    val kmPerDay: Double? = null,
    val daysSinceLast: Int? = null
) {
    fun getFormattedDate(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(date))
    }

    companion object {
        fun calculateKmSinceLastEntry(current: BatteryEntry, previous: BatteryEntry?): Double {
            return previous?.let {
                (current.totalKm - it.totalKm).coerceAtLeast(0.0)
            } ?: 0.0
        }

        fun calculateDaysSinceLastEntry(current: BatteryEntry, previous: BatteryEntry?): Int {
            return (previous?.let {
                val diffMillis = current.date - it.date
                TimeUnit.MILLISECONDS.toDays(diffMillis)
            } ?: 0).toInt()
        }

        fun calculateKmPerDay(km: Double, days: Int): Double {
            return if (days > 0) km / days else km
        }
    }
}
