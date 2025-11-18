package com.github.ringmydevice.data.repo

import android.content.Context
import com.github.ringmydevice.data.datastore.appDataStore
import com.github.ringmydevice.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SettingsRepository private constructor(context: Context) {
    private val dataStore = context.appDataStore

    suspend fun getRmdCommandKeyword(): String = withContext(Dispatchers.IO) {
        dataStore.data.first()[SettingsViewModel.RMD_COMMAND] ?: "rmd"
    }

    suspend fun getRingtoneUri(): String = withContext(Dispatchers.IO) {
        dataStore.data.first()[SettingsViewModel.RMD_RINGTONE] ?: ""
    }

    suspend fun isPinEnabled(): Boolean = withContext(Dispatchers.IO) {
        dataStore.data.first()[SettingsViewModel.RMD_PIN_ENABLED] ?: false
    }

    suspend fun getPin(): String = withContext(Dispatchers.IO) {
        dataStore.data.first()[SettingsViewModel.RMD_PIN] ?: ""
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
