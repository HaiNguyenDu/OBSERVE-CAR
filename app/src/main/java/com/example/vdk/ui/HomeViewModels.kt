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

@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)

    private val _latestSensor = MutableLiveData<Sensor>()
    val latestSensor: LiveData<Sensor> = _latestSensor

    private val _todaySensors = MutableLiveData<List<Sensor>>()
    val todaySensors: LiveData<List<Sensor>> = _todaySensors

    init {
        observeRealtimeData()
        getAllSensor()
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
        intervalMillis: Long = 2000L,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repeat(count) {
                val newSensor = Sensor(
                    sound = (1..1023).random().toDouble(),
                    temperature = (1..45).random().toDouble(),
                    time = 0.0,
                    weight = 0.0
                )
                repository.addData(newSensor)
                delay(intervalMillis)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchTodayData() {
        repository.getDataToday { list ->
            for (i in 0..list.size - 1) {
                list[i].time = i.toDouble()
            }
            _todaySensors.value = list

        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.unregisterObserveData()
    }

    fun getAllSensor() {
        viewModelScope.launch {
            _todaySensors.value = repository.getAllSensor()
        }
    }
}
