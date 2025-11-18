package com.github.ringmydevice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ringmydevice.data.model.CommandLog
import com.github.ringmydevice.data.repo.CommandRepository
import com.github.ringmydevice.di.AppGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CommandViewModel(
    private val repo: CommandRepository = AppGraph.commandRepo // default fake
) : ViewModel() {

    private val _logs = MutableStateFlow<List<CommandLog>>(emptyList())
    val logs: StateFlow<List<CommandLog>> = _logs

    fun refresh() {
        viewModelScope.launch { _logs.value = repo.latest() }
    }
}
