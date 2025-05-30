package com.example.vdk.ui.home

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val TAG = "MJPEG_DEBUG" // Thay đổi TAG để dễ theo dõi log
    private val viewModels: HomeViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }
    private val permissionPushNotification: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Xử lý kết quả cấp quyền ở đây nếu cần
            if (granted) {
                Log.d(TAG, "Notification permission granted.")
            } else {
                Log.d(TAG, "Notification permission denied.")
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root) // Chỉ gọi setContentView một lần
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.hide()
        // setContentView(binding.root) // Xóa dòng này, đã gọi ở trên

        obverseLiveData()
        setOnClick()
        permissionPushNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        // Không gọi setUpCamera() ở đây nữa, sẽ gọi trong onResume()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Setting up camera.")
        setUpCamera() // Thiết lập camera mỗi khi activity resume
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Stopping camera playback.")
        // Dừng playback khi activity không còn visible để giải phóng tài nguyên
        // và tránh lỗi khi không có surface hợp lệ.
        // Chạy trên luồng IO để tránh block UI thread nếu stopPlayback tốn thời gian.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                binding.mjpegView.stopPlayback()
                Log.d(TAG, "MjpegView playback stopped successfully in onPause.")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping MjpegView playback in onPause", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun obverseLiveData() {
        viewModels.latestSensor.observe(this) { sensor ->
            binding.apply {
                tvSound.text = sensor.getSoundFormat().toString()
                tvTemperature.text = sensor.temperatureToString()
                tvWeight.text = if (sensor.weight > 0) "There is an object in the car"
                else "No objects in the car"
                setUpState(sensor.getSoundFormat(), sensor.temperature)
            }
            val intent = Intent(this@MainActivity, FireBaseService::class.java).apply {
                putExtra("weight", sensor.weight)
            }
            startService(intent)
        }
    }

    private fun setOnClick() {
        binding.layoutTemperature.setOnClickListener {
            startActivity(Intent(this, DetailTemperatureActivity::class.java))
            // finish() // Cân nhắc có nên finish() MainActivity ở đây không. Nếu finish() thì khi back sẽ không quay lại MainActivity.
        }
        binding.layoutSound.setOnClickListener {
            startActivity(Intent(this, DetailSoundActivity::class.java))
        }
        binding.btnMuteAll.setOnClickListener {
            val intent = Intent(this, FireBaseService::class.java)
            intent.action = "offAll"
            startService(intent)
        }
        binding.btnMutePhone.setOnClickListener {
            val intent = Intent(this, FireBaseService::class.java)
            intent.action = "offCar"
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

                sound <= SOUND.MEDIUM -> { // Sửa điều kiện này, có thể bạn muốn sound > SOUND.NORMAL && sound <= SOUND.MEDIUM
                    tvSState.text = SOUND.MEDIUM_MESSAGE
                    tvSState.setTextColor(getColor(R.color.green)) // Nên là màu khác nếu mức độ khác nhau
                }

                sound > SOUND.MEDIUM && sound <= SOUND.HIGH -> { // Giả sử SOUND.MEDIUM < SOUND.HIGH
                    tvSState.text = SOUND.HIGH_MESSAGE
                    tvSState.setTextColor(getColor(R.color.red))
                }
                // Thêm trường hợp cho sound > SOUND.HIGH nếu có
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
                    tvTState.setTextColor(getColor(R.color.yellow))
                }

                temperature > TEMPERATURE.HIGH -> { // Xử lý trường hợp nhiệt độ rất cao (nguy hiểm)
                    tvTState.text = TEMPERATURE.DANGEROUS_MESSAGE
                    tvTState.setTextColor(getColor(R.color.red))
                }
                // else -> { // Trường hợp còn lại, ví dụ nhiệt độ quá thấp (TEMPERATURE.COOL hoặc thấp hơn)
                //    tvTState.text = "QUÁ LẠNH" // Hoặc một thông báo phù hợp
                //    tvTState.setTextColor(getColor(R.color.blue)) // Ví dụ
                // }
            }
        }
    }

    private fun setUpCamera() {
        Log.d(TAG, "setUpCamera: Attempting to stop previous playback.")
        // Dừng playback hiện tại (nếu có) trước khi bắt đầu stream mới.
        // Nên chạy trên luồng IO nếu có khả năng block, nhưng stopPlayback của thư viện này thường nhanh.
        // Thử gọi trực tiếp, nếu có vấn đề thì chuyển vào coroutine.
        try {
            binding.mjpegView.stopPlayback()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback in setUpCamera (อาจ không có gì để stop)", e)
        }


        binding.mjpegView.apply {
            setDisplayMode(DisplayMode.BEST_FIT) // Hoặc DisplayMode.FULLSCREEN tùy theo nhu cầu
            showFps(true)
            setFpsOverlayBackgroundColor(Color.DKGRAY)
            setFpsOverlayTextColor(Color.WHITE)
            // Đảm bảo MjpegView được hiển thị và tvCamera bị ẩn khi bắt đầu thiết lập
            visibility = View.VISIBLE
        }
        binding.tvCamera.visibility = View.GONE // Ẩn thông báo lỗi ban đầu

        val url = "http://192.168.110.179:81/stream"
        val timeoutSeconds = 10 // Tăng thời gian chờ lên một chút

        Log.d(TAG, "setUpCamera: Opening Mjpeg stream from $url")
        Mjpeg.newInstance()
            .open(url, timeoutSeconds)
            .subscribeOn(Schedulers.io()) // Thực hiện thao tác open() trên luồng I/O
            .observeOn(AndroidSchedulers.mainThread()) // Nhận kết quả trên luồng chính
            .subscribe(
                { inputStream ->
                    Log.d(TAG, "Mjpeg stream opened successfully. Setting source.")
                    binding.tvCamera.visibility = View.GONE // Ẩn thông báo lỗi nếu có
                    binding.mjpegView.visibility = View.VISIBLE
                    binding.mjpegView.setSource(inputStream)
                    // binding.mjpegView.startPlayback() // Một số thư viện có thể cần gọi startPlayback() sau khi setSource. Kiểm tra tài liệu thư viện Mjpeg.
                },
                { error ->
                    Log.e(TAG, "Failed to open Mjpeg stream", error)
                    binding.tvCamera.text = "Failed to connect to camera"
                    binding.tvCamera.visibility = View.VISIBLE
                    binding.mjpegView.visibility = View.GONE // Ẩn view camera khi lỗi
                }
            )
    }

    // Không cần override onResume() để stopPlayback nữa, vì onPause sẽ lo việc đó
    // và setUpCamera trong onResume sẽ tự động stop (nếu cần) rồi mới bắt đầu.
    // Xóa hàm onResume cũ của bạn nếu nó chỉ có stopPlayback.

    // override fun onStop() {
    //     super.onStop()
    //     // Bạn cũng có thể cân nhắc gọi stopPlayback ở onStop thay vì onPause
    //     // nếu muốn stream tiếp tục chạy khi có dialog hoặc activity trong suốt hiện lên.
    //     // Tuy nhiên, onPause thường là nơi tốt hơn để giải phóng tài nguyên khi activity không còn tương tác.
    // }

    // override fun onDestroy() {
    //    super.onDestroy()
    //    // Một số thư viện Mjpeg có thể có hàm Mjpeg.shutdown() hoặc mjpegView.release()
    //    // để giải phóng hoàn toàn tài nguyên. Kiểm tra tài liệu của thư viện.
    // }
}