package com.example.vdk.data

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.vdk.data.database.AppDatabase
import com.example.vdk.data.database.dao.SensorDao
import com.example.vdk.data.database.entity.SensorEntity
import com.example.vdk.model.Sensor
import com.example.vdk.utils.DATABASE
import com.example.vdk.utils.FIREBASE_URL
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class Repository(private val context: Context) {
    private val sensorDao: SensorDao
    private val database = FirebaseDatabase.getInstance(FIREBASE_URL).getReference(DATABASE)
    private var childEventListener: ChildEventListener? = null

    init {
        val appDatabase = AppDatabase.getInstant(context)
        sensorDao = appDatabase.sensorDao()
    }

    fun insertDataToRoom(sensors: List<Sensor>) {
        val listEntity: List<SensorEntity> = sensors.map {
            it.toEntity()
        }
    }

    fun deleteDataRoom(): Boolean {
        return sensorDao.deleteAllSensors() > 0
    }

    fun getAllSensor(): List<Sensor> {
        val sensors = sensorDao.getAllSensor()
        return sensors.map {
            it.toSensor()
        }
    }

    fun addData(sensor: Sensor): Boolean {
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        var result = false

        database.child(timestamp).setValue(sensor.toMap())
            .addOnSuccessListener { result = true }
            .addOnCanceledListener { result = false }

        return result
    }

    fun getLatestSensorData(callback: (Sensor) -> Unit) {
        database.orderByKey().limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val latestSnapshot = snapshot.children.firstOrNull()
                    val sensor =
                        latestSnapshot?.getValue(Sensor::class.java) ?: Sensor(0.0, 0.0, 0.0, 0.0)
                    callback(sensor)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(Sensor(0.0, 0.0, 0.0, 0.0))
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getDataToday(callback: (List<Sensor>) -> Unit) {
        val now = LocalDate.now()
        database.orderByKey()
        val startOfDayMillis = now.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        database.orderByKey()
            .startAt(startOfDayMillis.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val listData = snapshot.children.mapNotNull { child ->
                        child.getValue(Sensor::class.java)?.let { data ->
                            child.key?.toLongOrNull()?.let { ts ->
                                data.copy(time = epochMillisToHourDouble(ts).toDouble())
                            }
                        }
                    }
                    callback(listData)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }

    fun registerObserveData(callback: (Sensor) -> Unit) {
        childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(Sensor::class.java)?.let(callback)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        database.addChildEventListener(childEventListener!!)
    }

    fun unregisterObserveData() {
        childEventListener?.let {
            database.removeEventListener(it)
        }
        childEventListener = null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun epochMillisToHourDouble(epochMillis: Long): Int {
    val instant = Instant.ofEpochMilli(epochMillis)
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())

    val hour = zonedDateTime.hour
    val minute = zonedDateTime.minute
    val second = zonedDateTime.second
    val nano = zonedDateTime.nano

    return hour + ((minute / 60.0).toInt())
}
