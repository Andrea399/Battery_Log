package it.serielsrl.batterylog.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import it.serielsrl.batterylog.R
import it.serielsrl.batterylog.data.BatteryDatabase
import it.serielsrl.batterylog.data.BatteryRepository
import it.serielsrl.batterylog.databinding.ActivityMainBinding
import it.serielsrl.batterylog.ui.adapters.BatteryAdapter
import it.serielsrl.batterylog.utils.CsvHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            application,
            BatteryRepository(
                BatteryDatabase.getDatabase(application).batteryDao()
            )
        )
    }

    private lateinit var adapter: BatteryAdapter

    // EXPORT: SAF - Crea file
    private val exportCsvLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                lifecycleScope.launch {
                    val entries = viewModel.allEntries.value ?: emptyList()
                    CsvHelper.exportToCsvViaUri(this@MainActivity, uri, entries)
                }
            }
        }
    }

    // IMPORT: SAF - Seleziona file
    private val importCsvLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                lifecycleScope.launch {
                    viewModel.importFromCsv(this@MainActivity, uri) // uri da intent o SAF

                    //val imported = CsvHelper.importFromCsvViaUri(this@MainActivity, uri)
                    //viewModel.insertImportedEntries(imported)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        // RecyclerView
        adapter = BatteryAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        // Observe entries
        viewModel.allEntries.observe(this) { entries ->
            entries?.let { adapter.submitList(it) }
        }

        viewModel.operationStatus.observe(this) { success ->
            val message = if (success) getString(R.string.operation_success)
            else getString(R.string.operation_failed)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Aggiungi nuova voce
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddEntryActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_export -> {
                launchExportCsv()
                true
            }
            R.id.menu_import -> {
                launchImportCsv()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun launchExportCsv() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "battery_log_entries.csv")
        }
        exportCsvLauncher.launch(intent)
    }

    private fun launchImportCsv() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
        }
        importCsvLauncher.launch(intent)
    }
}
