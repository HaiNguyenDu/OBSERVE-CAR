package com.example.vdk.ui.home

import android.Manifest
import android.content.Intent
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
import com.example.vdk.DetailCameraActivity
import com.example.vdk.HistoryActivity
import com.example.vdk.R
import com.example.vdk.databinding.ActivityMainBinding
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
        setOnClick()
        permissionPushNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun obverseLiveData() {
        viewModels.latestTracking.observe(this) { tracking ->
            val state = tracking.state
            tracking?.timestamp?.let { timestampInSeconds ->
                val currentTimeInSeconds = System.currentTimeMillis() / 1000

                val differenceInSeconds = Math.abs(currentTimeInSeconds - timestampInSeconds)


                val timeAgoString = when {
                    differenceInSeconds < 60 -> {
                        "${differenceInSeconds.toLong()} giây trước"
                    }

                    differenceInSeconds < 3600 -> {
                        val minutes = (differenceInSeconds / 60).toLong()
                        "$minutes phút trước"
                    }

                    else -> {
                        val hours = (differenceInSeconds / 3600).toLong()
                        val remainingMinutes = ((differenceInSeconds % 3600) / 60).toLong()
                        "$hours giờ $remainingMinutes phút trước"
                    }
                }
                if (state == true) {
                    binding.tvTitle.text = "Phát hiện kẻ lạ - $timeAgoString"
                } else
                    binding.tvTitle.text = "Phát hiện chuyển động - $timeAgoString"

                Log.d("TimeCalculation", "Chuỗi thời gian đã định dạng: $timeAgoString")
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