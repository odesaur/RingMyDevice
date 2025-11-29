package com.github.ringmydevice.data.repo

import android.util.Base64
import com.github.ringmydevice.di.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Minimal HTTP client for the self-hosted RMD server.
 * Uses the same API shape as fmd-server (now rmd-server).
 */
class RmdServerRepository private constructor(
    private val settingsRepository: SettingsRepository
) {
    companion object {
        @Volatile
        private var INSTANCE: RmdServerRepository? = null

        fun getInstance(): RmdServerRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RmdServerRepository(AppGraph.settingsRepo).also { INSTANCE = it }
            }
        }
    }

    suspend fun fetchPendingCommand(): String? = withContext(Dispatchers.IO) {
        val config = settingsRepository.getServerConfig() ?: return@withContext null
        val payload = JSONObject().apply {
            put("IDT", config.accessToken)
            put("Data", "")
        }
        val url = URL("${config.baseUrl}/api/v1/command")
        val response = executeJsonRequest(url, payload, "PUT") ?: return@withContext null
        val data = response.optString("Data", "")
        if (data.isBlank()) null else data
    }

    suspend fun clearPendingCommand() = withContext(Dispatchers.IO) {
        val config = settingsRepository.getServerConfig() ?: return@withContext
        val payload = JSONObject().apply {
            put("IDT", config.accessToken)
            put("Data", "")
            put("UnixTime", 0)
            put("CmdSig", "")
        }
        val url = URL("${config.baseUrl}/api/v1/command")
        executeJsonRequest(url, payload, "POST")
    }

    suspend fun uploadPicture(imageBytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val config = settingsRepository.getServerConfig() ?: return@withContext false
        val payload = JSONObject().apply {
            put("IDT", config.accessToken)
            put("Data", Base64.encodeToString(imageBytes, Base64.NO_WRAP))
        }
        val url = URL("${config.baseUrl}/api/v1/picture")
        val response = executeJsonRequest(url, payload, "POST")
        response != null
    }

    suspend fun getServerVersion(baseUrl: String): String? = withContext(Dispatchers.IO) {
        val url = URL("${baseUrl.trimEnd('/')}/api/v1/version")
        val connection = (openConnection(url) as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        return@withContext try {
            val code = connection.responseCode
            if (code !in 200..299) return@withContext null
            connection.inputStream?.use { input ->
                val body = BufferedReader(InputStreamReader(input)).readText().trim()
                if (body.isBlank()) null else body // plain text version string
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    suspend fun verifyAccessToken(baseUrl: String, token: String): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("IDT", token)
            put("Data", "")
        }
        val url = URL("${baseUrl.trimEnd('/')}/api/v1/command")
        val response = executeJsonRequest(url, payload, "PUT")
        response != null
    }

    /**
     * Open a URLConnection. If it is HTTPS, install a permissive trust manager/hostname verifier.
     * This is intended for self-hosted LAN testing with self-signed certs. Do NOT use for production.
     */
    private fun openConnection(url: URL): java.net.URLConnection {
        val connection = url.openConnection()
        if (connection is HttpsURLConnection) {
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            val ssl = SSLContext.getInstance("SSL")
            ssl.init(null, trustAll, java.security.SecureRandom())
            connection.sslSocketFactory = ssl.socketFactory
            connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
        }
        return connection
    }

    private suspend fun executeJsonRequest(url: URL, payload: JSONObject, method: String): JSONObject? {
        // Try once, and on 401 try to refresh token (if remember password) and retry once.
        val baseUrl = "${url.protocol}://${url.authority}"
        var refreshed = false
        repeat(2) { attempt ->
            val connection = (openConnection(url) as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 15_000
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            try {
                connection.outputStream.use { os ->
                    os.write(payload.toString().toByteArray())
                }
                val code = connection.responseCode
                if (code == 401 && !refreshed && refreshTokenIfPossible(baseUrl)) {
                    refreshed = true
                    return@repeat // retry
                }
                if (code !in 200..299) {
                    return null
                }
                connection.inputStream?.use { input ->
                    val body = BufferedReader(InputStreamReader(input)).readText()
                    return if (body.isBlank()) JSONObject() else runCatching { JSONObject(body) }.getOrElse { JSONObject() }
                }
                return JSONObject()
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private suspend fun refreshTokenIfPossible(baseUrl: String): Boolean {
        val fullConfig = settingsRepository.getFullServerConfig() ?: return false
        if (!fullConfig.rememberPassword || fullConfig.storedPassword.isBlank() || fullConfig.userId.isBlank()) {
            return false
        }
        val newToken = login(baseUrl, fullConfig.userId, fullConfig.storedPassword)
        if (newToken.isNullOrBlank()) return false
        settingsRepository.setAccessToken(newToken)
        return true
    }

    suspend fun registerDevice(
        baseUrl: String,
        requestedId: String,
        password: String,
        registrationToken: String
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject().apply {
                if (requestedId.isNotBlank()) put("RequestedUsername", requestedId)
                put("PlainPassword", password)
                if (registrationToken.isNotBlank()) put("RegistrationToken", registrationToken)
            }
            val url = URL("${baseUrl.trimEnd('/')}/api/v1/device")
            val response = executeJsonRequest(url, json, "PUT") ?: return@withContext null
            response.optString("DeviceId", requestedId.ifBlank { "" })
        }.getOrNull()
    }

    suspend fun login(
        baseUrl: String,
        userId: String,
        password: String
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject().apply {
                put("IDT", userId)
                put("PlainPassword", password)
                put("SessionDurationSeconds", 7 * 24 * 60 * 60) // 1 week
            }
            val url = URL("${baseUrl.trimEnd('/')}/api/v1/requestAccess")
            val response = executeJsonRequest(url, json, "PUT") ?: return@withContext null
            response.optString("Data", "")
        }.getOrNull()
    }

    suspend fun deleteAccount(baseUrl: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject().apply {
                put("IDT", accessToken)
                put("Data", "")
            }
            val url = URL("${baseUrl.trimEnd('/')}/api/v1/device")
            val response = executeJsonRequest(url, json, "POST")
            response != null
        }.getOrDefault(false)
    }

    suspend fun changePassword(
        baseUrl: String,
        accessToken: String,
        newPassword: String
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject().apply {
                put("IDT", accessToken)
                put("PlainPassword", newPassword)
            }
            val url = URL("${baseUrl.trimEnd('/')}/api/v1/password")
            val response = executeJsonRequest(url, json, "POST")
            response != null
        }.getOrDefault(false)
    }

    suspend fun registerPushEndpoint(endpoint: String): Boolean = withContext(Dispatchers.IO) {
        val config = settingsRepository.getServerConfig() ?: return@withContext false
        val payload = JSONObject().apply {
            put("IDT", config.accessToken)
            put("Data", endpoint)
        }
        val url = URL("${config.baseUrl.trimEnd('/')}/api/v1/push")
        val response = executeJsonRequest(url, payload, "POST")
        response != null
    }
}
