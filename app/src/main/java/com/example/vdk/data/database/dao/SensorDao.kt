package com.example.vdk.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.vdk.data.database.entity.SensorEntity

@Dao
interface SensorDao {
    @Insert
    suspend fun insertSensors(sensor: SensorEntity)

    @Query("DELETE FROM sensor")
    suspend fun deleteAllSensors(): Int

    @Query("SELECT * From sensor")
    suspend fun getAllSensor(): List<SensorEntity>
}