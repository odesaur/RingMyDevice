package com.github.ringmydevice.data.repo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple in-memory allowed contacts list for Show & Tell 1.
 * Swap to DataStore/Room later.
 */
class AllowedContactsRepository private constructor() {

    private val _contacts = MutableStateFlow<List<String>>(emptyList())
    val contacts: StateFlow<List<String>> = _contacts.asStateFlow()

    /** Normalize to a canonical key: digits only for numbers, lowercase for names. */
    private fun normalize(s: String): String {
        val trimmed = s.trim()
        val digits = trimmed.filter { it.isDigit() }
        return if (digits.length >= 7) digits else trimmed.lowercase()
    }

    fun add(raw: String): Boolean {
        val n = normalize(raw)
        if (n.isBlank()) return false
        val exists = _contacts.value.any { normalize(it) == n }
        if (exists) return false
        _contacts.value = _contacts.value + raw.trim()
        return true
    }

    fun removeAt(index: Int) {
        if (index !in _contacts.value.indices) return
        _contacts.value = _contacts.value.toMutableList().apply { removeAt(index) }
    }

    fun remove(raw: String) {
        val n = normalize(raw)
        _contacts.value = _contacts.value.filterNot { normalize(it) == n }
    }

    fun clear() {
        _contacts.value = emptyList()
    }

    /** Check if given sender (e.g., "+1-604…", "Alice") is allowed. */
    fun isAllowed(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val n = normalize(sender)
        return _contacts.value.any { normalize(it) == n }
    }

    companion object {
        // simple singleton; replace with DI later
        val instance: AllowedContactsRepository by lazy { AllowedContactsRepository() }
    }
}
