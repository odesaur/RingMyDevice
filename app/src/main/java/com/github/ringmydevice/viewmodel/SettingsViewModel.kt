package com.github.ringmydevice.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.ringmydevice.data.datastore.appDataStore
import com.github.ringmydevice.ui.theme.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.appDataStore

    // define keys
    companion object {
        val RING_ENABLED = booleanPreferencesKey("ring_enabled")
        val LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
        val PHOTO_ENABLED = booleanPreferencesKey("photo_enabled")
        val TRUSTED_NUMBER = stringPreferencesKey("trusted_number")
        val SECRET_KEY = stringPreferencesKey("secret_key")
        val RMD_PIN_ENABLED = booleanPreferencesKey("rmd_pin_enabled")
        val RMD_PIN = stringPreferencesKey("rmd_pin")
        val RMD_COMMAND = stringPreferencesKey("rmd_command")
        val RMD_RINGTONE = stringPreferencesKey("rmd_ringtone")
        val RMD_LOCK_MESSAGE = stringPreferencesKey("rmd_lock_message")
        val SEND_SMS_FEEDBACK = booleanPreferencesKey("send_sms_feedback")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val FMD_SERVER_URL = stringPreferencesKey("fmd_server_url")
        val FMD_ACCESS_TOKEN = stringPreferencesKey("fmd_access_token")
        val FMD_UPLOAD_WHEN_ONLINE = booleanPreferencesKey("fmd_upload_when_online")
        val OPEN_CELL_ID_TOKEN = stringPreferencesKey("open_cell_id_token")
    }

    // read fata
    val ringEnabled = dataStore.data
        .map { it[RING_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val locationEnabled = dataStore.data
        .map { it[LOCATION_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val photoEnabled = dataStore.data
        .map { it[PHOTO_ENABLED] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val savedTrustedNumber = dataStore.data.map { it[TRUSTED_NUMBER] ?: "" }
    val savedSecretKey = dataStore.data.map { it[SECRET_KEY] ?: "" }
    val rmdPinEnabled = dataStore.data
        .map { it[RMD_PIN_ENABLED] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val rmdCommand = dataStore.data
        .map { it[RMD_COMMAND] ?: "rmd" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "rmd")
    val rmdRingtone = dataStore.data
        .map { it[RMD_RINGTONE] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val rmdLockMessage = dataStore.data
        .map { it[RMD_LOCK_MESSAGE] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val smsFeedbackEnabled = dataStore.data
        .map { it[SEND_SMS_FEEDBACK] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themePreference = dataStore.data
        .map { prefs -> ThemePreference.valueOf(prefs[THEME_PREFERENCE] ?: ThemePreference.SYSTEM.name) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreference.SYSTEM)

    val useDynamicColor = dataStore.data
        .map { it[USE_DYNAMIC_COLOR] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val fmdServerUrl = dataStore.data
        .map { it[FMD_SERVER_URL] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val fmdAccessToken = dataStore.data
        .map { it[FMD_ACCESS_TOKEN] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val fmdUploadWhenOnline = dataStore.data
        .map { it[FMD_UPLOAD_WHEN_ONLINE] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val openCellIdToken = dataStore.data
        .map { it[OPEN_CELL_ID_TOKEN] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // write Data
    fun setRingEnabled(value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[RING_ENABLED] = value } }
    }

    fun setLocationEnabled(value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[LOCATION_ENABLED] = value } }
    }

    fun setPhotoEnabled(value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PHOTO_ENABLED] = value } }
    }

    fun saveTextSettings(number: String, secret: String) {
        viewModelScope.launch {
            dataStore.edit {
                it[TRUSTED_NUMBER] = number
                it[SECRET_KEY] = secret
            }
        }
    }

    fun setRmdPinEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[RMD_PIN_ENABLED] = enabled } }
    }

    fun setRmdPin(pin: String) {
        viewModelScope.launch { dataStore.edit { it[RMD_PIN] = pin } }
    }

    fun setRmdCommand(command: String) {
        viewModelScope.launch { dataStore.edit { it[RMD_COMMAND] = command } }
    }

    fun setRmdRingtone(uri: String) {
        viewModelScope.launch { dataStore.edit { it[RMD_RINGTONE] = uri } }
    }

    fun setRmdLockMessage(message: String) {
        viewModelScope.launch { dataStore.edit { it[RMD_LOCK_MESSAGE] = message } }
    }

    fun setSmsFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[SEND_SMS_FEEDBACK] = enabled } }
    }

    fun setThemePreference(preference: ThemePreference) {
        viewModelScope.launch { dataStore.edit { it[THEME_PREFERENCE] = preference.name } }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[USE_DYNAMIC_COLOR] = enabled } }
    }

    fun setFmdServerUrl(url: String) {
        viewModelScope.launch { dataStore.edit { it[FMD_SERVER_URL] = url } }
    }

    fun setFmdAccessToken(token: String) {
        viewModelScope.launch { dataStore.edit { it[FMD_ACCESS_TOKEN] = token } }
    }

    fun setFmdUploadWhenOnline(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[FMD_UPLOAD_WHEN_ONLINE] = enabled } }
    }

    fun setOpenCellIdToken(token: String) {
        viewModelScope.launch { dataStore.edit { it[OPEN_CELL_ID_TOKEN] = token } }
    }
}
