package com.example.vdk.utils

import android.icu.text.SimpleDateFormat
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Date
import java.util.Locale

class TimeFormatter : ValueFormatter() {
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val timestamps = mutableListOf<Long>()

    fun setTimestamps(timestampList: List<Long>) {
        timestamps.clear()
        timestamps.addAll(timestampList)
    }

    override fun getFormattedValue(value: Float): String {
        val index = value.toInt()
        return if (index >= 0 && index < timestamps.size) {
            dateFormat.format(Date(timestamps[index]))
        } else {
            ""
        }
    }
}