package com.example.vdk.ui

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.vdk.data.Repository
import com.example.vdk.model.Sensor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)

    private val _latestSensor = MutableLiveData<Sensor>()
    val latestSensor: LiveData<Sensor> = _latestSensor

    private val _todaySensors = MutableLiveData<List<Sensor>>()
    val todaySensors: LiveData<List<Sensor>> = _todaySensors

    init {
        observeRealtimeData()
        fetchLatestSensor()
    }

    private fun observeRealtimeData() {
        repository.registerObserveData { sensor ->
            _latestSensor.value = sensor
            todaySensors.value?.size?.let {
                if (it > 0) {
                    fetchTodayData()
                }
            }
        }
    }

    private fun fetchLatestSensor() {
        repository.getLatestSensorData { sensor ->
            _latestSensor.postValue(sensor)
        }
    }

    fun addDataRandomly(
        count: Int = 200,
        intervalMillis: Long = 5000L,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repeat(count) {
                val newSensor = Sensor(
                    sound = (1..1023).random().toDouble(),
                    temperature = (1..45).random().toDouble(),
                    time = 0.0,
                    weight = 5.0
                )
                repository.addData(newSensor)
                delay(intervalMillis)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchTodayData() {
        viewModelScope.launch {
            val isOnline = hasInternetAccess()
            if (isOnline) {
                repository.getDataToday { list ->
                    for (i in 0..list.size - 1) {
                        list[i].time = i.toDouble()
                    }
                    _todaySensors.value = list
                }
                insertDataToRoom()
            } else getAllSensorRoom()

        }
    }

    fun insertDataToRoom() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = todaySensors.value ?: return@launch
            val Items = list
            repository.deleteDataRoom()
            repository.insertDataToRoom(Items)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun getAllSensorRoom() {
        viewModelScope.launch {
            val list = repository.getAllSensor()
            for (i in 0..list.size - 1) {
                list[i].time = i.toDouble()
            }
            _todaySensors.value = list
        }
    }

    suspend fun hasInternetAccess(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL("https://www.google.com")
            val connection = url.openConnection() as
                    HttpURLConnection
            connection.connectTimeout = 3000
            connection.connect()
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }
}
