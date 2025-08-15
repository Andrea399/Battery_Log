package it.serielsrl.batterylog.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import it.serielsrl.batterylog.R
import it.serielsrl.batterylog.data.BatteryDatabase
import it.serielsrl.batterylog.data.BatteryEntry
import it.serielsrl.batterylog.data.BatteryRepository
import it.serielsrl.batterylog.databinding.ActivityAddEntryBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddEntryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
    }

    private lateinit var binding: ActivityAddEntryBinding
    private lateinit var repository: BatteryRepository
    private var selectedDate = Calendar.getInstance().timeInMillis
    private var editingEntryId: Int? = null // ID entry che stiamo modificando

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = BatteryDatabase.getDatabase(application)
        repository = BatteryRepository(database.batteryDao())

        // Verifica se siamo in modalità modifica
        editingEntryId = intent.getIntExtra(EXTRA_ENTRY_ID, -1).takeIf { it != -1 }

        setupDatePicker()
        setupForm()

        // Se stiamo modificando, carica i dati
        if (editingEntryId != null) {
            loadEntryData(editingEntryId!!)
        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.etDate.setText(dateFormat.format(Date(selectedDate)))

        binding.etDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedDate = calendar.timeInMillis
                    binding.etDate.setText(dateFormat.format(Date(selectedDate)))
                    updateCalculations()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupForm() {
        binding.etTotalKm.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateCalculations()
        }

        binding.btnSave.setOnClickListener {
            saveEntry()
        }

        updateCalculations()
    }

    private fun loadEntryData(entryId: Int) {
        lifecycleScope.launch {
            val entry = repository.getEntryById(entryId)
            entry?.let {
                selectedDate = it.date
                binding.etDate.setText(
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it.date))
                )
                binding.etTotalKm.setText(it.totalKm.toString())
                updateCalculations()
            }
        }
    }

    private fun updateCalculations() {
        lifecycleScope.launch {
            val totalKmText = binding.etTotalKm.text.toString()
            if (totalKmText.isBlank()) return@launch

            val totalKm = totalKmText.toDoubleOrNull() ?: return@launch
            val previousEntry = repository.getPreviousEntry(selectedDate)

            val kmSinceLast = BatteryEntry.calculateKmSinceLastEntry(
                BatteryEntry(0, selectedDate, totalKm),
                previousEntry
            )
            val daysSinceLast = BatteryEntry.calculateDaysSinceLastEntry(
                BatteryEntry(0, selectedDate, totalKm),
                previousEntry
            )
            val kmPerDay = BatteryEntry.calculateKmPerDay(kmSinceLast, daysSinceLast)

            binding.tvKmSinceLast.text = getString(R.string.km_since_last, kmSinceLast)
            binding.tvDaysSinceLast.text = getString(R.string.days_since_last, daysSinceLast)
            binding.tvKmPerDay.text = getString(R.string.km_per_day, kmPerDay)
        }
    }

    private fun saveEntry() {
        val totalKmText = binding.etTotalKm.text.toString()
        if (totalKmText.isBlank()) {
            Toast.makeText(this, getString(R.string.enter_total_km), Toast.LENGTH_SHORT).show()
            return
        }

        val totalKm = totalKmText.toDoubleOrNull()
        if (totalKm == null) {
            Toast.makeText(this, getString(R.string.invalid_km_value), Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            if (editingEntryId != null) {
                // Aggiorna la entry e ricalcola tutte quelle successive
                repository.updateWithRecalculation(
                    BatteryEntry(
                        id = editingEntryId!!,
                        date = selectedDate,
                        totalKm = totalKm
                    )
                )
            } else {
                // Inserisce una nuova entry
                repository.insertWithCalculatedFields(
                    BatteryEntry(
                        date = selectedDate,
                        totalKm = totalKm
                    )
                )
            }
            finish()
        }
    }
}
