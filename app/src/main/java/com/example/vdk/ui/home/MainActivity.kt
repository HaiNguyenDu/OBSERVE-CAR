package com.example.vdk.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.bumptech.glide.Glide
import com.example.vdk.DetailCameraActivity
import com.example.vdk.HistoryActivity
import com.example.vdk.R
import com.example.vdk.databinding.ActivityMainBinding
import com.example.vdk.model.Tracking
import com.example.vdk.service.FireBaseService
import com.example.vdk.ui.HomeViewModel

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val TAG = "MJPEG_DEBUG"
    private val viewModels: HomeViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }
    private val permissionPushNotification: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            if (granted) {
                Log.d(TAG, "Notification permission granted.")
            } else {
                Log.d(TAG, "Notification permission denied.")
            }
        }
    private val handler = Handler(Looper.getMainLooper())
    private var updateUIRunnable: Runnable? = null
    private var latestTrackingData: Tracking? = null

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
        startService(Intent(this, FireBaseService::class.java).apply {
            putExtra("start",1)
        })
        obverseLiveData()
        setOnClick()
        permissionPushNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    override fun onResume() {
        super.onResume()
        // Bắt đầu hoặc tiếp tục cập nhật UI khi người dùng quay lại màn hình
        startAutoUpdate()
    }
    override fun onPause() {
        super.onPause()
        // Dừng cập nhật UI khi người dùng rời màn hình để tiết kiệm tài nguyên
        stopAutoUpdate()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun obverseLiveData() {
        viewModels.latestTracking.observe(this) { tracking ->
            updateUi(tracking)
            latestTrackingData = tracking
            startAutoUpdate()
        }
    }
    private fun startAutoUpdate() {
        stopAutoUpdate()
        if (latestTrackingData == null) return

        updateUIRunnable = Runnable {

            latestTrackingData?.let {
                updateUi(it)
            }

            handler.postDelayed(this.updateUIRunnable!!, 60000)
        }
        handler.post(updateUIRunnable!!)
    }
    private fun stopAutoUpdate() {
        updateUIRunnable?.let { handler.removeCallbacks(it) }
        updateUIRunnable = null
    }
    private fun updateUi(tracking: Tracking) {
        tracking.timestamp?.let { timestampInSeconds ->
            val currentTimeInSeconds = System.currentTimeMillis() / 1000
            val differenceInSeconds = Math.abs(currentTimeInSeconds - timestampInSeconds)
            val timeAgoString = when {
                differenceInSeconds < 60 -> "${differenceInSeconds.toLong()} giây trước"
                differenceInSeconds < 3600 -> "${(differenceInSeconds / 60).toLong()} phút trước"
                else -> {
                    val hours = (differenceInSeconds / 3600).toLong()
                    val remainingMinutes = ((differenceInSeconds % 3600) / 60).toLong()
                    "$hours giờ $remainingMinutes phút trước"
                }
            }


            if (tracking.state == true) {
                binding.tvTitle.text = "Phát hiện kẻ lạ - $timeAgoString"
            } else {
                binding.tvTitle.text = "Phát hiện chuyển động - $timeAgoString"
                binding.bgCard.setBackgroundResource(R.drawable.bg_green)
                binding.tvTitle.setTextColor(getColor(R.color.green))
                Glide.with(this)
                    .load(R.drawable.ic_people)
                    .into(binding.icHeader)
            }
        }
    }

    private fun setOnClick() {
        binding.btnHistory.setOnClickListener {
            startActivity(
                Intent(this, HistoryActivity::class.java)
            )
        }
        binding.layoutStream.setOnClickListener {
            startActivity(
                Intent(this, DetailCameraActivity::class.java)
            )

        }
    }

}