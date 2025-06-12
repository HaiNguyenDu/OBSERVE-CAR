package com.example.vdk

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vdk.databinding.ActivityHistoryBinding
import com.example.vdk.ui.HomeViewModel
import com.example.vdk.ui.history.ItemAdapter

class HistoryActivity : AppCompatActivity() {
    private val binding by lazy { ActivityHistoryBinding.inflate(layoutInflater) }
    private val viewModels: HomeViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    private lateinit var adapterTracking: ItemAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setUpUi()
        observeViewModel()
        viewModels.loadInitialData()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeViewModel() {

        viewModels.allTrackingData.observe(this) { list ->
            if(list.isNotEmpty())
            {
                binding.tvNoData.visibility =View.GONE
            }
            adapterTracking.updateData(list)
        }
    }

    private fun setUpUi() {
        adapterTracking = ItemAdapter(context = this, mutableListOf())

        binding.rcv.apply {
            layoutManager =
                LinearLayoutManager(this@HistoryActivity, LinearLayoutManager.VERTICAL, false)

            adapter = adapterTracking
        }
    }
}
