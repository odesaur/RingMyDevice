package com.github.ringmydevice.data.model

data class AppLogEntry(
    val timeMillis: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String,
    val message: String,
    val category: LogCategory = LogCategory.GENERAL
)

enum class LogLevel { DEBUG, INFO, WARN, ERROR }
enum class LogCategory { COMMAND, PERMISSION, SETTINGS, GENERAL }
