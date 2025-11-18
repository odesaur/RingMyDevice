package com.github.ringmydevice.data.repo

import com.github.ringmydevice.data.model.AllowedContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple in-memory allowed contacts list for Show & Tell 1.
 * Swap to DataStore/Room later.
 */
class AllowedContactsRepository private constructor() {

    private val _contacts = MutableStateFlow<List<AllowedContact>>(emptyList())
    val contacts: StateFlow<List<AllowedContact>> = _contacts.asStateFlow()

    private fun AllowedContact.sanitized(): AllowedContact =
        AllowedContact(name = name.trim(), phoneNumber = phoneNumber.trim())

    private fun normalizePhone(phone: String): String = phone.filter { it.isDigit() }
    private fun normalizeName(name: String): String = name.trim().lowercase()

    private fun normalize(contact: AllowedContact): String {
        val digits = normalizePhone(contact.phoneNumber)
        if (digits.isNotBlank()) return digits
        return normalizeName(contact.name)
    }

    fun add(contact: AllowedContact): Boolean {
        val sanitized = contact.sanitized()
        if (normalizePhone(sanitized.phoneNumber).isBlank()) return false
        val key = normalize(sanitized)
        if (key.isBlank()) return false
        val exists = _contacts.value.any { normalize(it) == key }
        if (exists) return false
        _contacts.value = _contacts.value + sanitized
        return true
    }

    fun removeAt(index: Int) {
        if (index !in _contacts.value.indices) return
        _contacts.value = _contacts.value.toMutableList().apply { removeAt(index) }
    }

    fun remove(raw: String) {
        if (raw.isBlank()) return
        val digits = normalizePhone(raw)
        if (digits.isNotBlank()) {
            _contacts.value = _contacts.value.filterNot { normalizePhone(it.phoneNumber) == digits }
            return
        }
        val normalizedName = normalizeName(raw)
        _contacts.value = _contacts.value.filterNot { normalizeName(it.name) == normalizedName }
    }

    fun clear() {
        _contacts.value = emptyList()
    }

    /** Check if given sender (e.g., "+1-604…", "Alice") is allowed. */
    fun isAllowed(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val digits = normalizePhone(sender)
        if (digits.isNotBlank()) {
            return _contacts.value.any { normalizePhone(it.phoneNumber) == digits }
        }
        val normalizedName = normalizeName(sender)
        if (normalizedName.isBlank()) return false
        return _contacts.value.any { normalizeName(it.name) == normalizedName }
    }

    companion object {
        // simple singleton; replace with DI later
        val instance: AllowedContactsRepository by lazy { AllowedContactsRepository() }
    }
}
