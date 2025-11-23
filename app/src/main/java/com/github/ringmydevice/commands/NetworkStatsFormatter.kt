package com.github.ringmydevice.commands

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import com.github.ringmydevice.permissions.Permissions
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.LinkedHashSet

data class NetworkStatsReport(
    val message: String,
    val permissionMissing: Boolean
)

object NetworkStatsFormatter {
    fun buildReport(context: Context): NetworkStatsReport {
        val builder = StringBuilder()
        builder.append("Network statistics:\n\n")
        appendDeviceIps(builder)
        builder.append("\n")
        val permissionMissing = appendWifiNetworks(context, builder)
        val message = builder.toString().trimEnd()
        return NetworkStatsReport(message = message, permissionMissing = permissionMissing)
    }

    private fun appendDeviceIps(builder: StringBuilder) {
        builder.append("Device IPs:\n")
        val interfaces: List<NetworkInterface> = runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
        }
            .getOrElse {
                builder.append("  Unable to read network interfaces\n")
                return
            }
        var added = false
        interfaces
            .filter { it.isUp && !it.isLoopback }
            .forEach { networkInterface ->
                val addresses: List<String> = networkInterface.inetAddresses.toList()
                    .mapNotNull { address ->
                        if (address.isLoopbackAddress || address.isLinkLocalAddress || address.isAnyLocalAddress) {
                            null
                        } else {
                            formatAddress(address).takeIf { it.isNotBlank() }
                        }
                    }
                if (addresses.isEmpty()) return@forEach
                added = true
                builder.append("Interface: ${networkInterface.displayName ?: networkInterface.name}\n")
                addresses.forEach { address ->
                    builder.append("  ")
                    builder.append(address)
                    builder.append("\n")
                }
            }
        if (!added) {
            builder.append("  none found\n")
        }
    }

    @SuppressLint("MissingPermission")
    private fun appendWifiNetworks(context: Context, builder: StringBuilder): Boolean {
        builder.append("Wifi networks:\n")
        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
            ?: run {
                builder.append("  Wi-Fi service unavailable\n")
                return false
            }
        val missingLocation = !Permissions.hasLocationPermission(context)
        val nearbyRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val missingNearby = nearbyRequired && !Permissions.hasNearbyWifiPermission(context)
        if (missingLocation || missingNearby) {
            val missing = mutableListOf<String>()
            if (missingLocation) missing.add("location")
            if (missingNearby) missing.add("nearby Wi-Fi")
            builder.append("  Permission missing: ${missing.joinToString(" and ")}\n")
            return true
        }
        if (!wifiManager.isWifiEnabled) {
            builder.append("  Wi-Fi is turned off\n")
            return false
        }

        val networks = LinkedHashSet<Pair<String, String>>()
        val connectionInfo = runCatching { wifiManager.connectionInfo }.getOrNull()
        if (connectionInfo != null) {
            val ssid = sanitizeSsid(connectionInfo.ssid)
            val bssid = connectionInfo.bssid
            if (ssid.isNotBlank() || !bssid.isNullOrBlank()) {
                networks.add(ssid.ifBlank { "(unknown)" } to (bssid ?: "unknown"))
            }
        }

        val scanResults = runCatching { wifiManager.scanResults }.getOrElse {
            builder.append("  Unable to read Wi-Fi networks\n")
            return false
        }
        scanResults.forEach { result ->
            val ssid = sanitizeSsid(result.SSID)
            val bssid = result.BSSID ?: "unknown"
            networks.add(ssid.ifBlank { "(hidden)" } to bssid)
        }

        if (networks.isEmpty()) {
            builder.append("  none found\n")
        } else {
            networks.forEach { (ssid, bssid) ->
                builder.append("SSID: $ssid\n")
                builder.append("BSSID: $bssid\n")
            }
        }
        return false
    }

    private fun formatAddress(address: InetAddress): String {
        val raw = address.hostAddress ?: return ""
        return if (address is Inet6Address) raw.substringBefore('%') else raw
    }

    private fun sanitizeSsid(ssid: String?): String {
        if (ssid.isNullOrBlank()) return ""
        val trimmed = ssid.trim('"')
        return if (trimmed.equals("<unknown ssid>", ignoreCase = true)) "" else trimmed
    }
}
