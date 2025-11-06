package com.github.ringmydevice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ringmydevice.di.AppGraph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AllowedContactsViewModel : ViewModel() {
    private val repo = AppGraph.allowedRepo

    val contacts: StateFlow<List<String>> =
        repo.contacts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun add(input: String, onDuplicate: (() -> Unit)? = null): Boolean {
        val ok = repo.add(input)
        if (!ok) onDuplicate?.invoke()
        return ok
    }

    fun removeAt(index: Int) = viewModelScope.launch { repo.removeAt(index) }
    fun clear() = viewModelScope.launch { repo.clear() }
}
