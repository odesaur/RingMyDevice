package com.github.ringmydevice.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "app_settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.dataStore

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
}
