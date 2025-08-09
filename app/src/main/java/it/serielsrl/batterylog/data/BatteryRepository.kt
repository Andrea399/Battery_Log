package it.serielsrl.batterylog.data

import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow

class BatteryRepository(private val batteryDao: BatteryDao) {
    val allEntries: Flow<List<BatteryEntry>> = batteryDao.getAllEntries()

    suspend fun insert(entry: BatteryEntry) {
        batteryDao.insert(entry)
    }

    // Calcola i campi prima di inserire
    suspend fun insertWithCalculatedFields(entry: BatteryEntry) {
        val lastEntry = batteryDao.getPreviousEntry()

        val kmSinceLast = BatteryEntry.calculateKmSinceLastEntry(entry, lastEntry)
        val daysSinceLast = BatteryEntry.calculateDaysSinceLastEntry(entry, lastEntry)
        val kmPerDay = BatteryEntry.calculateKmPerDay(kmSinceLast, daysSinceLast)

        val entryWithCalculatedFields = entry.copy(
            kmSinceLast = kmSinceLast,
            daysSinceLast = daysSinceLast.toInt(),
            kmPerDay = kmPerDay
        )

        batteryDao.insert(entryWithCalculatedFields)
    }


    suspend fun getPreviousEntry(beforeDate: Long): BatteryEntry? {
        return batteryDao.getEntryBeforeDate(beforeDate)
    }

    suspend fun deleteAll() {
        batteryDao.deleteAll()
    }

    fun getAllEntriesAsLiveData() = allEntries.asLiveData()
}
