package com.example.vdk.data

import android.content.Context
import android.os.Build
import android.util.Log
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

    suspend fun insertDataToRoom(sensors: List<Sensor>) {
        val listEntity: List<SensorEntity> = sensors.map {
            it.toEntity()
        }
        for (i in listEntity) {
            sensorDao.insertSensors(i)
        }
    }

    suspend fun deleteDataRoom(): Boolean {
        return sensorDao.deleteAllSensors() > 0
    }

    suspend fun getAllSensor(): List<Sensor> {
        try {
            val sensors = sensorDao.getAllSensor()
            Log.d("sensors", "sensors:" + sensors.size.toString())
            if (sensors.isEmpty())
                return emptyList()
            return sensors.map {
                it.toSensor()
            }
        } catch (e: Error) {
            Log.d("database", e.toString())
            emptyList<Sensor>()
        }
        return emptyList()
    }

    fun addData(sensor: Sensor): Boolean {
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        var result = false

        database.child(timestamp).setValue(sensor.toMap())
            .addOnSuccessListener { true }
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
