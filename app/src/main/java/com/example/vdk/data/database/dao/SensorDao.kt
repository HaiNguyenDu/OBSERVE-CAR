package com.example.vdk.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.vdk.data.database.entity.SensorEntity

@Dao
interface SensorDao {
    @Insert
    fun insertSensors(sensors: List<SensorEntity> = emptyList<SensorEntity>())

    @Query("DELETE FROM sensor")
    fun deleteAllSensors(): Int

    @Query("SELECT * From sensor")
    fun getAllSensor(): List<SensorEntity>
}