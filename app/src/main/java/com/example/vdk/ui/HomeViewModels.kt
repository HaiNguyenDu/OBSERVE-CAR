package com.example.vdk.ui

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.vdk.data.Repository
import com.example.vdk.model.Tracking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)


    private val _latestTracking = MutableLiveData<Tracking>()
    val latestTracking: LiveData<Tracking> = _latestTracking

    // LiveData cho danh sách tất cả các bản ghi theo dõi
    private val _allTrackingData = MutableLiveData<List<Tracking>>()
    val allTrackingData: LiveData<List<Tracking>> = _allTrackingData

    init {
        // Tải dữ liệu ban đầu khi ViewModel được tạo
        loadInitialData()
        // Bắt đầu lắng nghe các thay đổi thời gian thực
        observeRealtimeData()
    }

    /**
     * Tải dữ liệu ban đầu từ repository.
     */
    fun loadInitialData() {
        viewModelScope.launch {
            if (hasInternetAccess()) {
                // Lấy bản ghi mới nhất
                repository.getLatestTrackingData { tracking ->
                    _latestTracking.postValue(tracking)
                }
                // Lấy toàn bộ danh sách dữ liệu
                repository.getAllTrackingData { list ->
                    _allTrackingData.postValue(list)
                }
            } else {
                Log.w("HomeViewModel", "No internet access. Could not load initial data.")
                // Tại đây, bạn có thể xử lý logic khi không có mạng
                // ví dụ: hiển thị thông báo hoặc tải dữ liệu từ cache (nếu có)
            }
        }
    }

    /**
     * Đăng ký listener để nhận dữ liệu mới trong thời gian thực.
     */
    private fun observeRealtimeData() {
        repository.registerObserveData { newTracking ->
            // Cập nhật bản ghi mới nhất
            _latestTracking.postValue(newTracking)

            // Thêm bản ghi mới vào đầu danh sách hiện tại để cập nhật UI
            val currentList = _allTrackingData.value?.toMutableList() ?: mutableListOf()
            currentList.add(0, newTracking) // Thêm vào đầu danh sách
            _allTrackingData.postValue(currentList)
        }
    }

    /**
     * Dọn dẹp listener khi ViewModel bị hủy.
     */
    override fun onCleared() {
        super.onCleared()
        // Hủy đăng ký listener để tránh rò rỉ bộ nhớ
        repository.unregisterObserveData()
    }

    /**
     * Kiểm tra kết nối Internet.
     */
    private suspend fun hasInternetAccess(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Sử dụng một trang web đáng tin cậy để kiểm tra kết nối
            val url = URL("https://www.google.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.connect()
            connection.responseCode == 200
        } catch (e: IOException) {
            Log.e("HomeViewModel", "Internet check failed", e)
            false
        }
    }
}
