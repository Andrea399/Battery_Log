package it.serielsrl.batterylog.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import it.serielsrl.batterylog.data.BatteryEntry
import it.serielsrl.batterylog.data.BatteryRepository
import it.serielsrl.batterylog.utils.CsvHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    application: Application,
    private val repository: BatteryRepository
) : AndroidViewModel(application) {

    val allEntries: LiveData<List<BatteryEntry>> = repository.getAllEntriesAsLiveData()
    private val _operationStatus = MutableLiveData<Boolean>()
    val operationStatus: LiveData<Boolean> = _operationStatus
    fun importFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val entries = CsvHelper.importFromCsvViaUri(context, uri)
                if (entries.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        repository.deleteAll()
                        entries.forEach { repository.insert(it) }
                    }
                }
                _operationStatus.postValue(true)
            } catch (e: Exception) {
                _operationStatus.postValue(false)
            }
        }
    }


}

class MainViewModelFactory(
    private val application: Application,
    private val repository: BatteryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}