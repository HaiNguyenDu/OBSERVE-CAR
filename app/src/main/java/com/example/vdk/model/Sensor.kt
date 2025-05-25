package com.example.vdk.model

import com.example.vdk.data.database.entity.SensorEntity
import kotlin.math.roundToInt

data class Sensor(
    val sound: Double = 0.0,
    val temperature: Double = 0.0,
    var time: Double = 0.0,
    val weight: Double = 0.0,
) {
    fun temperatureToString(): String {
        return "$temperature ºC"
    }

    fun soundToString(): String {
        return (((sound * 10) / 102.3).toDouble().roundToInt() / 10).toString()
    }

    fun getSoundFormat(): Double {
        return (((sound * 10) / 102.3).toDouble().roundToInt().toDouble() / 10)
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "temperature" to temperature,
            "sound" to sound
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