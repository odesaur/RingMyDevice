package com.github.ringmydevice.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.github.ringmydevice.data.datastore.appDataStore
import com.github.ringmydevice.ui.theme.ThemePreference
import com.github.ringmydevice.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SettingsRepository private constructor(context: Context) {
    private val dataStore = context.appDataStore

    suspend fun getRmdCommandKeyword(): String = withContext(Dispatchers.IO) {
        dataStore.data.first()[SettingsViewModel.RMD_COMMAND] ?: "rmd"
    }

    suspend fun getRingtoneUri(): String = withContext(Dispatchers.IO) {
        dataStore.data.first()[SettingsViewModel.RMD_RINGTONE] ?: ""
    }

    suspend fun isRingEnabled(): Boolean = withContext(Dispatchers.IO) {
        dataStore.data.first()[SettingsViewModel.RING_ENABLED] ?: true
    }

    suspend fun isSmsFeedbackEnabled(): Boolean = withContext(Dispatchers.IO) {
        dataStore.data.first()[SettingsViewModel.SEND_SMS_FEEDBACK] ?: true
    }

    suspend fun isPinEnabled(): Boolean = withContext(Dispatchers.IO) {
        dataStore.data.first()[SettingsViewModel.RMD_PIN_ENABLED] ?: false
    }

    suspend fun getPin(): String = withContext(Dispatchers.IO) {
        dataStore.data.first()[SettingsViewModel.RMD_PIN] ?: ""
    }

    suspend fun readSnapshot(): SettingsSnapshot = withContext(Dispatchers.IO) {
        val prefs = dataStore.data.first()
        SettingsSnapshot(
            rmdCommand = prefs[SettingsViewModel.RMD_COMMAND] ?: "rmd",
            rmdRingtone = prefs[SettingsViewModel.RMD_RINGTONE] ?: "",
            rmdLockMessage = prefs[SettingsViewModel.RMD_LOCK_MESSAGE] ?: "",
            sendSmsFeedback = prefs[SettingsViewModel.SEND_SMS_FEEDBACK] ?: true,
            themePreference = prefs[SettingsViewModel.THEME_PREFERENCE] ?: ThemePreference.SYSTEM.name,
            useDynamicColor = prefs[SettingsViewModel.USE_DYNAMIC_COLOR] ?: true
        )
    }

    suspend fun applySnapshot(snapshot: SettingsSnapshot) {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[SettingsViewModel.RMD_COMMAND] = snapshot.rmdCommand
                prefs[SettingsViewModel.RMD_RINGTONE] = snapshot.rmdRingtone
                prefs[SettingsViewModel.RMD_LOCK_MESSAGE] = snapshot.rmdLockMessage
                prefs[SettingsViewModel.SEND_SMS_FEEDBACK] = snapshot.sendSmsFeedback
                prefs[SettingsViewModel.THEME_PREFERENCE] = snapshot.themePreference
                prefs[SettingsViewModel.USE_DYNAMIC_COLOR] = snapshot.useDynamicColor
            }
        }
    }

    suspend fun getServerConfig(): ServerConfig? = withContext(Dispatchers.IO) {
        val prefs = dataStore.data.first()
        val url = prefs[SettingsViewModel.FMD_SERVER_URL]?.trim()?.trimEnd('/')
        val token = prefs[SettingsViewModel.FMD_ACCESS_TOKEN]?.trim()
        if (url.isNullOrBlank() || token.isNullOrBlank()) return@withContext null
        ServerConfig(baseUrl = url, accessToken = token)
    }

    companion object {
        const val DEFAULT_RING_SECONDS = 15
        const val LONG_RING_SECONDS = 30

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

data class SettingsSnapshot(
    val rmdCommand: String,
    val rmdRingtone: String,
    val rmdLockMessage: String,
    val sendSmsFeedback: Boolean,
    val themePreference: String,
    val useDynamicColor: Boolean
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("rmdCommand", rmdCommand)
        put("rmdRingtone", rmdRingtone)
        put("rmdLockMessage", rmdLockMessage)
        put("sendSmsFeedback", sendSmsFeedback)
        put("themePreference", themePreference)
        put("useDynamicColor", useDynamicColor)
    }

    companion object {
        fun fromJson(json: JSONObject): SettingsSnapshot = SettingsSnapshot(
            rmdCommand = json.optString("rmdCommand", "rmd"),
            rmdRingtone = json.optString("rmdRingtone", ""),
            rmdLockMessage = json.optString("rmdLockMessage", ""),
            sendSmsFeedback = json.optBoolean("sendSmsFeedback", true),
            themePreference = json.optString("themePreference", ThemePreference.SYSTEM.name),
            useDynamicColor = json.optBoolean("useDynamicColor", true)
        )
    }
}

data class ServerConfig(
    val baseUrl: String,
    val accessToken: String
)
