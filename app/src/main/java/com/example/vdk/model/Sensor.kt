package com.example.vdk.model

import com.example.vdk.data.database.entity.SensorEntity
import kotlin.math.roundToInt

data class Sensor(
    val sound: Double = 0.0,
    val temperature: Double = 0.0,
    var time: Long = 0,
    val weight: Double = 0.0,
) {
    fun temperatureToString(): String {
        val value = ((temperature * 10).toInt().toDouble()) / 10
        return "$value º"
    }

    fun getSoundFormat(): Double {
        return (((sound * 10) / 102.3).roundToInt().toDouble() / 10)
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "temperature" to temperature,
            "sound" to sound,
            "weight" to weight
        )
    }

    fun toEntity(): SensorEntity {
        return SensorEntity(
            time = time.toLong(),
            temperature = temperature,
            sound = sound.toLong(),
            weight = 0.0
        )
    }

}