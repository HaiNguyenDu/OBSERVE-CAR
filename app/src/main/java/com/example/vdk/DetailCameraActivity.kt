package com.example.vdk

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.vdk.databinding.ActivityDetailCameraBinding
import com.github.niqdev.mjpeg.DisplayMode
import com.github.niqdev.mjpeg.Mjpeg
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class DetailCameraActivity : AppCompatActivity() {
    private val binding by lazy { ActivityDetailCameraBinding.inflate(layoutInflater) }
    private val TAG = "MJPEG_DEBUG"

    private var mjpegSubscription: Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
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
        Log.d(TAG, "onPause: Stopping camera playback and unsubscribing.")

        mjpegSubscription?.unsubscribe()
        mjpegSubscription = null

        try {
            binding.mjpegView.stopPlayback()
            Log.d(TAG, "MjpegView playback stopped successfully in onPause.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MjpegView playback in onPause", e)
        }
    }

    private fun setUpCamera() {
        val url = "http://192.168.52.252:8000/stream"
        val timeoutSeconds = 10

        binding.mjpegView.setDisplayMode(DisplayMode.BEST_FIT)
        binding.mjpegView.showFps(true)

        Log.d(TAG, "setUpCamera: Opening Mjpeg stream from $url")
        mjpegSubscription = Mjpeg.newInstance()
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
                    binding.tvCamera.text = "Không thể kết nối tới camera"
                    binding.tvCamera.visibility = View.VISIBLE
                    binding.mjpegView.visibility = View.GONE
                }
            )
    }
}
