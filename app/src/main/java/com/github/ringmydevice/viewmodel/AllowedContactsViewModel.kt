package com.github.ringmydevice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ringmydevice.data.model.AllowedContact
import com.github.ringmydevice.di.AppGraph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AllowedContactsViewModel : ViewModel() {
    private val repo = AppGraph.allowedRepo

    val contacts: StateFlow<List<AllowedContact>> =
        repo.contacts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun add(name: String, phoneNumber: String, onDuplicate: (() -> Unit)? = null): Boolean {
        val contact = AllowedContact(name = name, phoneNumber = normalizePhone(phoneNumber))
        val ok = repo.add(contact)
        if (!ok) onDuplicate?.invoke()
        return ok
    }

    fun removeAt(index: Int) = viewModelScope.launch { repo.removeAt(index) }
    fun clear() = viewModelScope.launch { repo.clear() }

    private fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isBlank()) return raw.trim()
        return when {
            raw.trim().startsWith("+") && digits.isNotEmpty() -> "+$digits"
            digits.length == 10 -> "+1$digits"
            digits.length == 11 && digits.startsWith("1") -> "+$digits"
            else -> "+$digits"
        }
    }

    fun displayNumber(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.startsWith("1") && digits.length == 11) {
            val national = digits.drop(1)
            return formatNanp("1", national)
        }
        if (digits.length == 10) {
            return formatNanp("1", digits)
        }
        return raw
    }

    private fun formatNanp(country: String, national: String): String =
        if (national.length == 10) {
            "+$country (${national.take(3)}) ${national.substring(3, 6)}-${national.takeLast(4)}"
        } else {
            "+$country$national"
        }
}
