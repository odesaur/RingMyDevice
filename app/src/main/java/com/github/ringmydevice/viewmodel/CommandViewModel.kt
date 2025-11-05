package com.github.ringmydevice.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ringmydevice.data.model.CommandLog
import com.github.ringmydevice.data.model.CommandType
import com.github.ringmydevice.data.repo.CommandRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CommandViewModel(
    private val repo: CommandRepository = CommandRepository.fake() // default fake
) : ViewModel() {

    private val _logs = MutableStateFlow<List<CommandLog>>(emptyList())
    val logs: StateFlow<List<CommandLog>> = _logs


    fun simulateRing(seconds: Int = 5, context: Context) {
        viewModelScope.launch {
            repo.log(
                CommandLog(
                    type = CommandType.RING,
                    timestamp = System.currentTimeMillis(),
                    notes = "Simulated ring $seconds s"
                )
            )
            _logs.value = repo.latest()
        }
    }

    fun simulateLocate() {
        val fake = "49.2827, -123.1207 (Vancouver)"
        viewModelScope.launch {
            repo.log(
                CommandLog(
                    type = CommandType.LOCATE,
                    timestamp = System.currentTimeMillis(),
                    notes = "Simulated locate -> $fake"
                )
            )
            _logs.value = repo.latest()
        }
    }

    fun refresh() {
        viewModelScope.launch { _logs.value = repo.latest() }
    }
}
