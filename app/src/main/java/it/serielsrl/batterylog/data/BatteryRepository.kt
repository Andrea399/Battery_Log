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
    suspend fun updateWithRecalculation(entry: BatteryEntry) {
        // Prende tutte le entries ordinate per data
        val allEntries = batteryDao.getAllSortedByDate().toMutableList()

        // Trova la posizione della entry modificata
        val index = allEntries.indexOfFirst { it.id == entry.id }
        if (index == -1) return // se non trovata, esce

        // Sostituisce l'entry modificata nella lista
        allEntries[index] = entry

        // Ricalcola da quella modificata fino all'ultima
        for (i in index until allEntries.size) {
            val prev = if (i > 0) allEntries[i - 1] else null
            val updated = allEntries[i].copy(
                kmSinceLast = BatteryEntry.calculateKmSinceLastEntry(allEntries[i], prev),
                daysSinceLast = BatteryEntry.calculateDaysSinceLastEntry(allEntries[i], prev),
                kmPerDay = BatteryEntry.calculateKmPerDay(
                    BatteryEntry.calculateKmSinceLastEntry(allEntries[i], prev),
                    BatteryEntry.calculateDaysSinceLastEntry(allEntries[i], prev)
                )
            )

            // Aggiorna il DB
            batteryDao.update(updated)

            // Aggiorna anche la lista in memoria per il ciclo
            allEntries[i] = updated
        }
    }




    suspend fun getPreviousEntry(beforeDate: Long): BatteryEntry? {
        return batteryDao.getEntryBeforeDate(beforeDate)
    }
    suspend fun getEntryById(id: Long): BatteryEntry? {
        return batteryDao.getEntryById(id)
    }


    suspend fun deleteAll() {
        batteryDao.deleteAll()
    }

    fun getAllEntriesAsLiveData() = allEntries.asLiveData()
}
