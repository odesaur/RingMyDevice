package com.github.ringmydevice.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.ringmydevice.data.dao.LogDao
import com.github.ringmydevice.data.model.CommandLog
import com.github.ringmydevice.data.Converters

@Database(entities = [CommandLog::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ring_my_device_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}