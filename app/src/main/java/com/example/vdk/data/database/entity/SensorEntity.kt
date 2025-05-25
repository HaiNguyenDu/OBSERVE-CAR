package com.example.vdk.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.vdk.model.Sensor

@Entity(
    tableName = "sensor",
    indices = [
        Index("time", unique = true)
    ]
)
class SensorEntity(
    @PrimaryKey
    val time: Long,
    val temperature: Double,
    val sound: Long,
    val weight: Double,
) {
    fun toSensor(): Sensor {
        return Sensor(
            sound = this.sound.toDouble(),
            temperature = this.temperature,
            time = this.time.toDouble(),
            weight = this.weight
        )
    }
}