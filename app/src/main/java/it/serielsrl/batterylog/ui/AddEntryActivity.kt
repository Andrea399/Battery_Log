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
    private lateinit var binding: ActivityAddEntryBinding
    private lateinit var repository: BatteryRepository
    private var selectedDate = Calendar.getInstance().timeInMillis

    // Variabili per memorizzare i calcoli
    private var kmSinceLast: Double = 0.0
    private var daysSinceLast: Int = 0
    private var kmPerDay: Double = 0.0
    private var totalKm: Double =0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = BatteryDatabase.getDatabase(application)
        repository = BatteryRepository(database.batteryDao())

        setupDatePicker()
        setupForm()
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

    private fun updateCalculations() {
        lifecycleScope.launch {
            val totalKmText = binding.etTotalKm.text.toString()
            if (totalKmText.isBlank()) return@launch

            totalKm = totalKmText.toDoubleOrNull() ?: return@launch
            val previousEntry = repository.getPreviousEntry(selectedDate)

            kmSinceLast = BatteryEntry.calculateKmSinceLastEntry(
                BatteryEntry(0, selectedDate, totalKm),
                previousEntry
            )
            daysSinceLast = BatteryEntry.calculateDaysSinceLastEntry(
                BatteryEntry(0, selectedDate, totalKm),
                previousEntry
            )
            kmPerDay = BatteryEntry.calculateKmPerDay(kmSinceLast, daysSinceLast)

            binding.tvKmSinceLast.text = getString(R.string.km_since_last, kmSinceLast)
            binding.tvDaysSinceLast.text = getString(R.string.days_since_last, daysSinceLast)
            binding.tvKmPerDay.text = getString(R.string.km_per_day, kmPerDay)
        }
    }
    private fun saveEntry() {
        /*val totalKmText = binding.etTotalKm.text.toString()
        if (totalKmText.isBlank()) {
            Toast.makeText(this, getString(R.string.enter_total_km), Toast.LENGTH_SHORT).show()
            return
        }
*/
        //totalKm = totalKmText.toDoubleOrNull()
        if (totalKm == 0.toDouble()) {
            Toast.makeText(this, getString(R.string.invalid_km_value), Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val entry = BatteryEntry(
                date = selectedDate,
                totalKm = totalKm,
                kmSinceLast= kmSinceLast,
                daysSinceLast = daysSinceLast,
                kmPerDay = kmPerDay
                // Gli altri valori verranno calcolati automaticamente
            )

            //repository.insertWithCalculatedFields(entry)
            repository.insert(entry)

            finish()
        }
    }

}