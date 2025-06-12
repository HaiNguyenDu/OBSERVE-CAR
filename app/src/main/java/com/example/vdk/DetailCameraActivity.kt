package com.example.vdk

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.vdk.databinding.ActivityDetailCameraBinding
import com.github.niqdev.mjpeg.DisplayMode
import com.github.niqdev.mjpeg.Mjpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class DetailCameraActivity : AppCompatActivity() {
    private val binding by lazy { ActivityDetailCameraBinding.inflate(layoutInflater) }
    private val TAG = "MJPEG_DEBUG"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail_camera)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Setting up camera.")
        setUpCamera()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Stopping camera playback.")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                binding.mjpegView.stopPlayback()
                Log.d(TAG, "MjpegView playback stopped successfully in onPause.")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping MjpegView playback in onPause", e)
            }
        }
    }

    private fun setUpCamera() {
        Log.d(TAG, "setUpCamera: Attempting to stop previous playback.")
        try {
            binding.mjpegView.stopPlayback()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback in setUpCamera (อาจ không có gì để stop)", e)
        }


        binding.mjpegView.apply {
            setDisplayMode(DisplayMode.BEST_FIT)
            showFps(true)
            setFpsOverlayBackgroundColor(Color.DKGRAY)
            setFpsOverlayTextColor(Color.WHITE)

            visibility = View.VISIBLE
        }
        binding.tvCamera.visibility = View.GONE

        val url = "http://192.168.52.252:8000/stream"
        val timeoutSeconds = 10

        Log.d(TAG, "setUpCamera: Opening Mjpeg stream from $url")
        Mjpeg.newInstance()
            .open(url, timeoutSeconds)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { inputStream ->
                    Log.d(TAG, "Mjpeg stream opened successfully. Setting source.")
                    binding.tvCamera.visibility = View.GONE
                    binding.mjpegView.visibility = View.VISIBLE
                    binding.mjpegView.setSource(inputStream)

                },
                { error ->
                    Log.e(TAG, "Failed to open Mjpeg stream", error)
                    binding.tvCamera.text = "Failed to connect to camera"
                    binding.tvCamera.visibility = View.VISIBLE
                    binding.mjpegView.visibility = View.GONE
                }
            )
    }

}