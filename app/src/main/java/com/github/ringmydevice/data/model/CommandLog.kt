package com.github.ringmydevice.data.model

data class CommandLog(
    val id: Long = 0L,
    val type: CommandType = CommandType.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis(),
    val from: String? = null,
    val success: Boolean = true,
    val notes: String? = null,
    val lat: Double? = null,
    val lon: Double? = null
)
