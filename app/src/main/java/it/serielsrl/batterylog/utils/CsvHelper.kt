package it.serielsrl.batterylog.utils

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import it.serielsrl.batterylog.R
import it.serielsrl.batterylog.data.BatteryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvHelper {
    private const val FILENAME = "battery_log_entries.csv"
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    /**
     * Esportazione CSV con SAF
     */
    suspend fun exportToCsvViaUri(context: Context, uri: Uri, entries: List<BatteryEntry>) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                CSVWriter(outputStream.writer()).use { writer ->
                    writeCsvHeader(writer)
                    entries.forEachIndexed { index, entry ->
                        writeCsvEntry(writer, entry, entries.getOrNull(index + 1))
                    }
                }
            }
            showExportSuccess(context, uri.toString())
        } catch (e: Exception) {
            showExportError(context, e.message)
        }
    }

    /**
     * Importazione CSV con SAF
     */
    suspend fun importFromCsvViaUri(context: Context, uri: Uri): List<BatteryEntry> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                CSVReader(InputStreamReader(inputStream)).use { csvReader ->
                    val entries = parseCsvEntries(csvReader)
                    showImportSuccess(context, entries.size)
                    return@withContext entries.sortedBy { it.date }
                }
            } ?: run {
                showImportError(context, "Impossibile aprire il file")
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            showImportError(context, e.message)
            return@withContext emptyList()
        }
    }

    /**
     * Scrive l'intestazione del CSV
     */
    private fun writeCsvHeader(writer: CSVWriter) {
        writer.writeNext(
            arrayOf(
                "Date",
                "Total Km",
                "Km Since Last Charge",
                "Days Since Last Charge",
                "Km Per Day"
            )
        )
    }

    /**
     * Scrive una riga del CSV
     */
    private fun writeCsvEntry(writer: CSVWriter, entry: BatteryEntry, previousEntry: BatteryEntry?) {
        val kmSinceLast = BatteryEntry.calculateKmSinceLastEntry(entry, previousEntry)
        val daysSinceLast = BatteryEntry.calculateDaysSinceLastEntry(entry, previousEntry)
        val kmPerDay = BatteryEntry.calculateKmPerDay(kmSinceLast, daysSinceLast)

        writer.writeNext(
            arrayOf(
                dateFormat.format(Date(entry.date)),
                entry.totalKm.toString(),
                kmSinceLast.toString(),
                daysSinceLast.toString(),
                kmPerDay.toString()
            )
        )
    }

    /**
     * Parsing delle righe CSV in BatteryEntry
     */
    private fun parseCsvEntries(csvReader: CSVReader): MutableList<BatteryEntry> {
        val entries = mutableListOf<BatteryEntry>()
        val lines = csvReader.readAll().drop(1) // salta intestazione

        for (line in lines) {
            try {
                if (line.size >= 5) {
                    val date = dateFormat.parse(line[0].replace("\"", "").trim())?.time ?: continue
                    val totalKm = line[1].replace("\"", "").replace(",", ".").toDoubleOrNull() ?: continue
                    val kmSinceLast = line[2].replace("\"", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                    val daysSinceLast = line[3].replace("\"", "").toIntOrNull() ?: 0
                    val kmPerDay = line[4].replace("\"", "").replace(",", ".").toDoubleOrNull() ?: 0.0

                    val entry = BatteryEntry(
                        date = date,
                        totalKm = totalKm,
                        kmSinceLast = kmSinceLast,
                        daysSinceLast = daysSinceLast,
                        kmPerDay = kmPerDay
                    )

                    entries.add(entry)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return entries
    }

    /**
     * Toast di successo esportazione
     */
    private suspend fun showExportSuccess(context: Context, filePath: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                context.getString(R.string.export_success, filePath),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Toast errore esportazione
     */
    private suspend fun showExportError(context: Context, errorMessage: String?) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                context.getString(R.string.export_failed, errorMessage ?: "Errore sconosciuto"),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Toast di successo importazione
     */
    private suspend fun showImportSuccess(context: Context, count: Int) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                context.getString(R.string.import_success, count),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Toast errore importazione
     */
    private suspend fun showImportError(context: Context, errorMessage: String?) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                context.getString(R.string.import_failed, errorMessage ?: "Errore sconosciuto"),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
