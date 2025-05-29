package com.example.vdk.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.vdk.data.database.dao.SensorDao
import com.example.vdk.data.database.entity.SensorEntity


@Database(
    entities = [
        SensorEntity::class,
    ],
    exportSchema = false,
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao

    companion object {
        @Volatile
        private var INSTANT: AppDatabase? = null

        fun getInstant(context: Context): AppDatabase {
            return INSTANT ?: synchronized(this) {

                val instant = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vdk"
                ).fallbackToDestructiveMigration(false).build()

                INSTANT = instant
                instant
            }
        }
    }
}
