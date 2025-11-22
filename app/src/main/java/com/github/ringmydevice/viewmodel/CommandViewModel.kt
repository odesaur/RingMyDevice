package com.github.ringmydevice.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.ringmydevice.data.database.AppDatabase
import com.github.ringmydevice.data.model.CommandLog
import com.github.ringmydevice.data.model.CommandType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CommandViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).logDao()

    // expose logs as a StateFlow
    val logs: StateFlow<List<CommandLog>> = dao.getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // write logs to database
    fun addLog(type: CommandType, notes: String) {
        viewModelScope.launch {
            dao.insertLog(
                CommandLog(
                    type = type,
                    notes = notes,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // clear log entries
    fun clearLogs() {
        viewModelScope.launch {
            dao.clearLogs()
        }
    }
}