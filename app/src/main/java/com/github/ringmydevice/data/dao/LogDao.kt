package com.github.ringmydevice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.github.ringmydevice.data.model.CommandLog
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM command_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<CommandLog>>

    @Insert
    suspend fun insertLog(log: CommandLog)

    @Query("DELETE FROM command_logs")
    suspend fun clearLogs()
}