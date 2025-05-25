package com.example.vdk.ui.detail_sound

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.vdk.R
import com.example.vdk.databinding.ActivityDetailSoundBinding
import com.example.vdk.model.Sensor
import com.example.vdk.ui.HomeViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class DetailSoundActivity : AppCompatActivity() {
    private val binding by lazy { ActivityDetailSoundBinding.inflate(layoutInflater) }
    private val viewModel: HomeViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        obverseLiveData()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun obverseLiveData() {
        viewModel.todaySensors.observe(this) { list ->
            setUpChartDay(list)
            var result = 0.0
            list.forEach {
                result += it.getSoundFormat()
            }
            val av: Double = (((result / (list.size - 1)) * 100).toInt().toDouble()) / 100
            binding.tvAv.text = av.toString()
        }
        viewModel.latestSensor.observe(this) { sensor ->
            binding.apply {
                tvSound.text = sensor.getSoundFormat().toString()
            }
        }
        viewModel.fetchTodayData()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setUpChartDay(list: List<Sensor>) {
        if (list.isEmpty()) {
            binding.chart.clear()
            return
        }
        val listEntry = list.map {
            Entry(it.time.toFloat(), it.getSoundFormat().toFloat())
        }
        val dataSet = LineDataSet(listEntry, "Nhiệt độ")
        dataSet.color = getColor(R.color.red)
        dataSet.valueTextSize = 12f
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)

        val lineData = LineData(dataSet)
        binding.chart.data = lineData

        binding.chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.chart.axisRight.isEnabled = false

        binding.chart.invalidate()
    }
}