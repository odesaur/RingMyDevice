package com.github.ringmydevice.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_logs")
data class CommandLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: CommandType = CommandType.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis(),
    val from: String? = null,
    val success: Boolean = true,
    val notes: String? = null
)