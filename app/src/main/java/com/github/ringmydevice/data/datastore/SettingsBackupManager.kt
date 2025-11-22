package com.github.ringmydevice.data.datastore

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backs up and restores all app settings to/from a JSON document.
 *
 * This includes:
 *  - General (ring/location/photo, trusted number, shared secret)
 *  - FMD Server (url, token, upload behaviour)
 *  - Appearance (theme & dynamic colors)
 *  - Allowed contacts (snapshot list)
 *  - Logs (snapshot list)
 *
 * Allowed contacts and logs are passed in/out via lambdas
 *
 * ! NEEDS TO BE ALTERED DEPENDING ON IMPLEMENTATION OF REMAINING FEATURES !
 */
class SettingsBackupManager(
    private val dataStore: DataStore<Preferences>
) {

    // ---- Preference keys ----
    companion object {
        val RING_ENABLED = booleanPreferencesKey("ring_enabled")
        val LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
        val PHOTO_ENABLED = booleanPreferencesKey("photo_enabled")
        val TRUSTED_NUMBER = stringPreferencesKey("trusted_number")
        val SHARED_SECRET = stringPreferencesKey("shared_secret")

        val SERVER_URL = stringPreferencesKey("server_url")
        val SERVER_TOKEN = stringPreferencesKey("server_token")
        val UPLOAD_WHEN_ONLINE = booleanPreferencesKey("upload_when_online")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
    }

    // ---- Snapshots ----

    data class AllowedContactSnapshot(
        val name: String,
        val phoneNumber: String
    )

    data class LogEntrySnapshot(
        val timestamp: Long,
        val type: String,
        val notes: String? = null
    )

    // ---- Export ----
    private suspend fun buildJson(
        getAllowedContacts: suspend () -> List<AllowedContactSnapshot>,
        getLogs: suspend () -> List<LogEntrySnapshot>
    ): JSONObject {
        val prefs = dataStore.data.first()
        val root = JSONObject()

        // General
        val general = JSONObject().apply {
            put("ringEnabled", prefs[RING_ENABLED] ?: true)
            put("locationEnabled", prefs[LOCATION_ENABLED] ?: true)
            put("photoEnabled", prefs[PHOTO_ENABLED] ?: false)
            put("trustedNumber", prefs[TRUSTED_NUMBER] ?: "")
            put("sharedSecret", prefs[SHARED_SECRET] ?: "")
        }
        root.put("general", general)

        // FMD server
        val fmd = JSONObject().apply {
            put("serverUrl", prefs[SERVER_URL] ?: "")
            put("serverToken", prefs[SERVER_TOKEN] ?: "")
            put("uploadWhenOnline", prefs[UPLOAD_WHEN_ONLINE] ?: true)
        }
        root.put("fmdServer", fmd)

        // Appearance
        val appearance = JSONObject().apply {
            put("themeMode", prefs[THEME_MODE] ?: "system")
            put("dynamicColors", prefs[DYNAMIC_COLORS] ?: true)
        }
        root.put("appearance", appearance)

        // Allowed contacts
        val contactsArray = JSONArray()
        getAllowedContacts().forEach { c ->
            contactsArray.put(
                JSONObject().apply {
                    put("name", c.name)
                    put("phoneNumber", c.phoneNumber)
                }
            )
        }
        root.put("allowedContacts", contactsArray)

        // Logs
        val logsArray = JSONArray()
        getLogs().forEach { log ->
            logsArray.put(
                JSONObject().apply {
                    put("timestamp", log.timestamp)
                    put("type", log.type)
                    put("notes", log.notes ?: JSONObject.NULL)
                }
            )
        }
        root.put("logs", logsArray)

        return root
    }

    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        getAllowedContacts: suspend () -> List<AllowedContactSnapshot>,
        getLogs: suspend () -> List<LogEntrySnapshot>
    ) {
        val json = buildJson(getAllowedContacts, getLogs).toString(2)

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.bufferedWriter().use { writer ->
                writer.write(json)
            }
        } ?: error("Could not open output stream for $uri")
    }

    // ---- Import ----
    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        replaceAllowedContacts: suspend (List<AllowedContactSnapshot>) -> Unit,
        replaceLogs: suspend (List<LogEntrySnapshot>) -> Unit
    ) {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().use { it.readText() }
        } ?: error("Could not open input stream for $uri")

        val root = JSONObject(text)

        // General
        val general = root.optJSONObject("general")
        val fmd = root.optJSONObject("fmdServer")
        val appearance = root.optJSONObject("appearance")

        dataStore.edit { prefs ->
            general?.let { g ->
                prefs[RING_ENABLED] = g.optBoolean("ringEnabled", prefs[RING_ENABLED] ?: true)
                prefs[LOCATION_ENABLED] = g.optBoolean("locationEnabled", prefs[LOCATION_ENABLED] ?: true)
                prefs[PHOTO_ENABLED] = g.optBoolean("photoEnabled", prefs[PHOTO_ENABLED] ?: false)
                prefs[TRUSTED_NUMBER] = g.optString("trustedNumber", prefs[TRUSTED_NUMBER] ?: "")
                prefs[SHARED_SECRET] = g.optString("sharedSecret", prefs[SHARED_SECRET] ?: "")
            }

            fmd?.let { f ->
                prefs[SERVER_URL] = f.optString("serverUrl", prefs[SERVER_URL] ?: "")
                prefs[SERVER_TOKEN] = f.optString("serverToken", prefs[SERVER_TOKEN] ?: "")
                prefs[UPLOAD_WHEN_ONLINE] = f.optBoolean("uploadWhenOnline", prefs[UPLOAD_WHEN_ONLINE] ?: true)
            }

            appearance?.let { a ->
                prefs[THEME_MODE] = a.optString("themeMode", prefs[THEME_MODE] ?: "system")
                prefs[DYNAMIC_COLORS] =
                    a.optBoolean("dynamicColors", prefs[DYNAMIC_COLORS] ?: true)
            }
        }

        // Allowed contacts
        val contactsArray = root.optJSONArray("allowedContacts")
        if (contactsArray != null) {
            val contacts = mutableListOf<AllowedContactSnapshot>()
            for (i in 0 until contactsArray.length()) {
                val obj = contactsArray.optJSONObject(i) ?: continue
                val name = obj.optString("name", "")
                val phone = obj.optString("phoneNumber", "")
                if (phone.isNotBlank()) {
                    contacts += AllowedContactSnapshot(name = name, phoneNumber = phone)
                }
            }
            replaceAllowedContacts(contacts)
        }

        // Logs
        val logsArray = root.optJSONArray("logs")
        if (logsArray != null) {
            val logs = mutableListOf<LogEntrySnapshot>()
            for (i in 0 until logsArray.length()) {
                val obj = logsArray.optJSONObject(i) ?: continue
                val ts = obj.optLong("timestamp", 0L)
                val type = obj.optString("type", "")
                val notes = obj.optString("notes", null)
                if (type.isNotBlank()) {
                    logs += LogEntrySnapshot(timestamp = ts, type = type, notes = notes)
                }
            }
            replaceLogs(logs)
        }
    }
}