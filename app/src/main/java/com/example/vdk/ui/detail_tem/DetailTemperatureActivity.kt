package com.example.vdk.ui.detail_tem

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.vdk.R
import com.example.vdk.databinding.ActivityDetailTemperatureBinding
import com.example.vdk.model.Sensor
import com.example.vdk.service.FireBaseService
import com.example.vdk.ui.HomeViewModel
import com.example.vdk.ui.home.MainActivity
import com.example.vdk.utils.TimeFormatter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class DetailTemperatureActivity : AppCompatActivity() {
    private val binding by lazy { ActivityDetailTemperatureBinding.inflate(layoutInflater) }
    private val viewModel: HomeViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

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
        supportActionBar?.hide()
        obverseLiveData()
        setUpButton()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun obverseLiveData() {
        viewModel.todaySensors.observe(this) { list ->
            setUpChartDay(list)
            var result = 0.0
            list.forEach {
                result += it.temperature
            }
            val av: Double = (((result / list.size) * 100).toInt().toDouble()) / 100
            binding.tvAv.text = av.toString()
        }
        viewModel.latestSensor.observe(this) { sensor ->
            binding.apply {
                tvTemperature.text = sensor.temperatureToString()
            }
            val intent = Intent(this@DetailTemperatureActivity, FireBaseService::class.java).apply {
                putExtra("weight", sensor.weight)
            }
            startService(intent)
        }
        viewModel.fetchTodayData()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setUpChartDay(list: List<Sensor>) {
        if (list.isEmpty()) {
            binding.chart.clear()
            return
        }

        // Tạo entries với index thay vì timestamp
        val listEntry = list.mapIndexed { index, sensor ->
            Entry(index.toFloat(), sensor.temperature.toFloat())
        }

        // Lấy danh sách timestamps
        val timestamps = list.map { it.time }

        val dataSet = LineDataSet(listEntry, "Nhiệt độ")
        dataSet.color = ContextCompat.getColor(binding.root.context, R.color.red)
        dataSet.valueTextSize = 12f
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.lineWidth = 2f

        val lineData = LineData(dataSet)
        binding.chart.data = lineData

        // Cấu hình trục X với TimeFormatter
        val xAxis = binding.chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        // Tạo và cấu hình TimeFormatter
        val timeFormatter = TimeFormatter()
        timeFormatter.setTimestamps(timestamps)
        xAxis.valueFormatter = timeFormatter

        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.setLabelCount(5, false) // Hiển thị tối đa 5 labels để tránh chen chúc
        xAxis.labelRotationAngle = -45f // Xoay labels 45 độ để dễ đọc


        binding.chart.axisRight.isEnabled = false
        val yAxisLeft = binding.chart.axisLeft
        yAxisLeft.setDrawGridLines(true)

        binding.chart.legend.isEnabled = true
        binding.chart.description.isEnabled = false
        binding.chart.setTouchEnabled(true)
        binding.chart.isDragEnabled = true
        binding.chart.setScaleEnabled(true)

        // Refresh
        binding.chart.notifyDataSetChanged()
        binding.chart.invalidate()
    }

    private fun setUpButton() {
        binding.btnBack.setOnClickListener {
            startActivity(Intent(this,MainActivity::class.java))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        super.onDestroy()
        viewModel.insertDataToRoom()
    }
}