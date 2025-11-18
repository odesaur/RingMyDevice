package com.github.ringmydevice.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.ringmydevice.data.datastore.appDataStore
import com.github.ringmydevice.data.model.AllowedContact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

class AllowedContactsRepository private constructor(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = context.appDataStore
    private val keyAllowedContacts = stringPreferencesKey("allowed_contacts_json")

    private val _contacts = MutableStateFlow<List<AllowedContact>>(emptyList())
    val contacts: StateFlow<List<AllowedContact>> = _contacts.asStateFlow()
    private val temporaryAllow = mutableMapOf<String, Long>()

    init {
        scope.launch {
            dataStore.data.collectLatest { prefs ->
                val raw = prefs[keyAllowedContacts]
                _contacts.value = raw?.let { decodeContacts(it) } ?: emptyList()
            }
        }
    }

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
        persistContacts(_contacts.value + sanitized)
        return true
    }

    fun removeAt(index: Int) {
        if (index !in _contacts.value.indices) return
        val updated = _contacts.value.toMutableList().apply { removeAt(index) }
        persistContacts(updated)
    }

    fun remove(raw: String) {
        if (raw.isBlank()) return
        val digits = normalizePhone(raw)
        val updated = if (digits.isNotBlank()) {
            _contacts.value.filterNot { normalizePhone(it.phoneNumber) == digits }
        } else {
            val normalizedName = normalizeName(raw)
            _contacts.value.filterNot { normalizeName(it.name) == normalizedName }
        }
        persistContacts(updated)
    }

    fun clear() {
        persistContacts(emptyList())
    }

    /** Check if given sender (e.g., "+1-604…", "Alice") is allowed. */
    fun isAllowed(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val key = normalizeRaw(sender)
        if (key.isBlank()) return false
        cleanupTemporary(now = System.currentTimeMillis())
        if (temporaryAllow[key]?.let { it > System.currentTimeMillis() } == true) {
            return true
        }
        return _contacts.value.any { normalize(it) == key }
    }

    fun grantTemporaryAccess(raw: String, durationMillis: Long = TEMP_ALLOW_DURATION_MS) {
        val key = normalizeRaw(raw)
        if (key.isBlank()) return
        val expiry = System.currentTimeMillis() + durationMillis
        synchronized(temporaryAllow) {
            temporaryAllow[key] = expiry
        }
    }

    private fun persistContacts(list: List<AllowedContact>) {
        _contacts.value = list
        scope.launch {
            dataStore.edit { prefs ->
                if (list.isEmpty()) {
                    prefs.remove(keyAllowedContacts)
                } else {
                    prefs[keyAllowedContacts] = encodeContacts(list)
                }
            }
        }
    }

    private fun encodeContacts(list: List<AllowedContact>): String {
        val array = JSONArray()
        list.forEach { contact ->
            array.put(
                JSONObject().apply {
                    put("name", contact.name)
                    put("phone", contact.phoneNumber)
                }
            )
        }
        return array.toString()
    }

    private fun decodeContacts(raw: String): List<AllowedContact> =
        runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val obj = array.getJSONObject(index)
                    add(
                        AllowedContact(
                            name = obj.optString("name"),
                            phoneNumber = obj.optString("phone")
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }

    private fun normalizeRaw(raw: String): String {
        val digits = normalizePhone(raw)
        if (digits.isNotBlank()) return digits
        return normalizeName(raw)
    }

    private fun cleanupTemporary(now: Long) {
        synchronized(temporaryAllow) {
            val iterator = temporaryAllow.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value <= now) iterator.remove()
            }
        }
    }

    companion object {
        private val TEMP_ALLOW_DURATION_MS = TimeUnit.MINUTES.toMillis(10)
        @Volatile
        private var INSTANCE: AllowedContactsRepository? = null

        fun getInstance(context: Context): AllowedContactsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AllowedContactsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
