package com.github.ringmydevice.data.repo

import android.content.Context
import com.github.ringmydevice.data.model.AppLogEntry
import com.github.ringmydevice.data.model.LogCategory
import com.github.ringmydevice.data.model.LogLevel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface LogsRepository {
    suspend fun log(entry: AppLogEntry)
    suspend fun recent(limit: Int = 200): List<AppLogEntry>
}

class FileLogsRepository(private val context: Context) : LogsRepository {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun log(entry: AppLogEntry) {
        withContext(Dispatchers.IO) {
            val file = fileFor(entry.timeMillis)
            val array = readArray(file)
            array.put(entry.toJson())
            writeArray(file, array)
        }
    }

    override suspend fun recent(limit: Int): List<AppLogEntry> = withContext(Dispatchers.IO) {
        val files = context.filesDir
            .listFiles { _, name -> name.startsWith("fmd-logs-") && name.endsWith(".json") }
            ?.sortedByDescending { it.name }
            .orEmpty()

        val entries = mutableListOf<AppLogEntry>()
        files.forEach { file ->
            if (entries.size >= limit) return@forEach
            val array = readArray(file)
            for (i in 0 until array.length()) {
                entries.add(array.getJSONObject(i).toEntry())
            }
        }
        entries.sortByDescending { it.timeMillis }
        entries.take(limit)
    }

    private fun fileFor(timeMillis: Long): File {
        val date = Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val name = "fmd-logs-${formatter.format(date)}.json"
        return File(context.filesDir, name)
    }

    private fun readArray(file: File): JSONArray {
        if (!file.exists()) return JSONArray()
        return runCatching {
            FileInputStream(file).bufferedReader().use { reader ->
                val text = reader.readText()
                if (text.isBlank()) JSONArray() else JSONArray(text)
            }
        }.getOrElse { JSONArray() }
    }

    private fun writeArray(file: File, array: JSONArray) {
        FileOutputStream(file).bufferedWriter().use { writer ->
            writer.write(array.toString())
        }
    }
}

private fun AppLogEntry.toJson(): JSONObject =
    JSONObject().apply {
        put("timeMillis", timeMillis)
        put("level", level.name)
        put("tag", tag)
        put("message", message)
        put("category", category.name)
    }

private fun JSONObject.toEntry(): AppLogEntry =
    AppLogEntry(
        timeMillis = optLong("timeMillis", System.currentTimeMillis()),
        level = optString("level").toLogLevel(),
        tag = optString("tag", "Unknown"),
        message = optString("message", ""),
        category = optString("category").toLogCategory()
    )

private fun String.toLogLevel(): LogLevel =
    runCatching { LogLevel.valueOf(this) }.getOrDefault(LogLevel.INFO)

private fun String.toLogCategory(): LogCategory =
    runCatching { LogCategory.valueOf(this) }.getOrDefault(LogCategory.GENERAL)
