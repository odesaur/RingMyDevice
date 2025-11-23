package com.github.ringmydevice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ringmydevice.data.model.AppLogEntry
import com.github.ringmydevice.data.model.LogCategory
import com.github.ringmydevice.data.repo.LogsRepository
import com.github.ringmydevice.di.AppGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppLogViewModel(
    private val repo: LogsRepository = AppGraph.logsRepo
) : ViewModel() {
    private val _logs = MutableStateFlow<List<AppLogEntry>>(emptyList())
    val logs: StateFlow<List<AppLogEntry>> = _logs

    fun refresh() {
        viewModelScope.launch {
            _logs.value = repo.recent()
        }
    }

    fun filter(category: LogCategory?): List<AppLogEntry> =
        _logs.value.filter { category == null || it.category == category }
}
