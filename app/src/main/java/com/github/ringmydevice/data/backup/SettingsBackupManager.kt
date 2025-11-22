package com.github.ringmydevice.data.backup

import android.content.Context
import android.net.Uri
import com.github.ringmydevice.data.model.AllowedContact
import com.github.ringmydevice.data.repo.SettingsSnapshot
import com.github.ringmydevice.di.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter

object SettingsBackupManager {
    suspend fun export(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = AppGraph.settingsRepo.readSnapshot()
            val contacts = AppGraph.allowedRepo.snapshot()
            val payload = JSONObject().apply {
                put("version", 1)
                put("settings", snapshot.toJson())
                put(
                    "allowedContacts",
                    JSONArray().apply {
                        contacts.forEach { contact ->
                            put(
                                JSONObject().apply {
                                    put("name", contact.name)
                                    put("phone", contact.phoneNumber)
                                }
                            )
                        }
                    }
                )
            }
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString(2))
                }
            } ?: throw IllegalStateException("Unable to open destination")
        }
    }

    suspend fun import(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("Unable to read file")
            val payload = JSONObject(json)
            val snapshot = SettingsSnapshot.fromJson(payload.getJSONObject("settings"))
            AppGraph.settingsRepo.applySnapshot(snapshot)
            val contactsArray = payload.optJSONArray("allowedContacts")
            val contacts = mutableListOf<AllowedContact>()
            if (contactsArray != null) {
                for (i in 0 until contactsArray.length()) {
                    val obj = contactsArray.getJSONObject(i)
                    contacts.add(
                        AllowedContact(
                            name = obj.optString("name"),
                            phoneNumber = obj.optString("phone")
                        )
                    )
                }
            }
            AppGraph.allowedRepo.replaceAll(contacts)
        }
    }
}
