package com.github.ringmydevice.data

import androidx.room.TypeConverter
import com.github.ringmydevice.data.model.CommandType

class Converters {
    @TypeConverter
    fun fromCommandType(value: CommandType): String {
        return value.name
    }

    @TypeConverter
    fun toCommandType(value: String): CommandType {
        return try {
            CommandType.valueOf(value)
        } catch (_: IllegalArgumentException) {
            CommandType.UNKNOWN
        }
    }
}