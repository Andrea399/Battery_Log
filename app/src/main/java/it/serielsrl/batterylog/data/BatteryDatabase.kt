package it.serielsrl.batterylog.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Database(entities = [BatteryEntry::class], version = 2, exportSchema = false)
abstract class BatteryDatabase : RoomDatabase() {
    abstract fun batteryDao(): BatteryDao

    companion object {
        @Volatile
        private var INSTANCE: BatteryDatabase? = null

        fun getDatabase(context: Context): BatteryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    "battery_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Dao
interface BatteryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BatteryEntry)

    @Query("SELECT * FROM battery_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<BatteryEntry>>
/*
    @Query("SELECT * FROM battery_entries WHERE id != (SELECT MAX(id) FROM battery_entries) ORDER BY date DESC LIMIT 1")
    suspend fun getPreviousEntry(): BatteryEntry?
*/
    @Query("SELECT * FROM battery_entries ORDER BY date DESC LIMIT 1")
    suspend fun getPreviousEntry(): BatteryEntry?

    @Query("DELETE FROM battery_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM battery_entries WHERE date < :beforeDate ORDER BY date DESC LIMIT 1")
    suspend fun getEntryBeforeDate(beforeDate: Long): BatteryEntry?

    @Query("SELECT * FROM battery_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Int): BatteryEntry?

    @Update
    suspend fun update(entry: BatteryEntry)

    @Query("SELECT * FROM battery_entries ORDER BY date ASC")
    suspend fun getAllSortedByDate(): List<BatteryEntry>

}