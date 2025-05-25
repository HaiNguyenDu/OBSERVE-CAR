package com.example.vdk.ui.home

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.vdk.R
import com.example.vdk.databinding.ActivityMainBinding
import com.example.vdk.service.FireBaseService
import com.example.vdk.ui.HomeViewModel
import com.example.vdk.ui.detail_sound.DetailSoundActivity
import com.example.vdk.ui.detail_tem.DetailTemperatureActivity
import com.example.vdk.utils.SOUND
import com.example.vdk.utils.TEMPERATURE
import com.github.niqdev.mjpeg.DisplayMode
import com.github.niqdev.mjpeg.Mjpeg
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val TAG = "MJPEG"
    private val viewModels: HomeViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }
    private val permissionPushNotification: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            when {

            }
        }

    fun startService() {
        val intent = Intent(this, FireBaseService::class.java)
        startService(intent)
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
        setContentView(binding.root)
        obverseLiveData()
        setOnClick()
        startService()
        permissionPushNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun obverseLiveData() {
        viewModels.latestSensor.observe(this) { sensor ->
            binding.apply {
                tvSound.text = sensor.soundToString()
                tvTemperature.text = sensor.temperatureToString()
                setUpState(sensor.getSoundFormat(), sensor.temperature)
            }
        }
    }

    private fun setOnClick() {
        binding.layoutTemperature.setOnClickListener {
            startActivity(Intent(this, DetailTemperatureActivity::class.java))
        }
        binding.layoutSound.setOnClickListener {
            startActivity(Intent(this, DetailSoundActivity::class.java))
        }
        binding.btn.setOnClickListener {
            val intent = Intent(this, FireBaseService::class.java)
            intent.action = "off"
            startService(intent)
        }
    }

    private fun setUpState(sound: Double, temperature: Double) {
        binding.apply {
            when {
                sound <= SOUND.NORMAL -> {
                    tvSState.text = SOUND.NORMAL_MESSAGE
                    tvSState.setTextColor(getColor(R.color.green))
                }

                sound <= SOUND.MEDIUM -> {
                    tvSState.text = SOUND.MEDIUM_MESSAGE
                    tvSState.setTextColor(getColor(R.color.green))
                }
    
                sound <= SOUND.HIGH -> {
                    tvSState.text = SOUND.HIGH_MESSAGE
                    tvSState.setTextColor(getColor(R.color.red))
                }
            }
            when {
                temperature <= TEMPERATURE.LOW &&
                        temperature > TEMPERATURE.COOL -> {
                    tvTState.text = TEMPERATURE.LOW_MESSAGE
                    tvTState.setTextColor(getColor(R.color.cold))
                }

                temperature <= TEMPERATURE.NORMAL &&
                        temperature > TEMPERATURE.LOW -> {
                    tvTState.text = TEMPERATURE.NORMAL_MESSAGE
                    tvTState.setTextColor(getColor(R.color.green))
                }

                temperature <= TEMPERATURE.HIGH &&
                        temperature > TEMPERATURE.NORMAL -> {
                    tvTState.text = TEMPERATURE.HIGH_MESSAGE
                    tvTState.setTextColor(getColor(R.color.green))
                }

                else -> {
                    tvTState.text = TEMPERATURE.DANGEROUS_MESSAGE
                    tvTState.setTextColor(getColor(R.color.red))
                }
            }
        }
    }

    private fun setUpCamera() {
        binding.mjpegView.apply {
            setDisplayMode(DisplayMode.BEST_FIT)
            showFps(true)
            setFpsOverlayBackgroundColor(Color.DKGRAY)
            setFpsOverlayTextColor(Color.WHITE)
        }

        // 2. Mở stream MJPEG
        val url = "http://192.168.10.58:81/stream"
        val timeoutSeconds = 5

        Mjpeg.newInstance()
            .open(url, timeoutSeconds)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ inputStream ->
                // Khi có InputStream, gán vào view
                binding.mjpegView.setSource(inputStream)
            }, { error ->
                Log.e(TAG, "Không mở được stream", error)
            })
    }

    override fun onPause() {
        super.onPause()
        binding.mjpegView.stopPlayback()
    }
}