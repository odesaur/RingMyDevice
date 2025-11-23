package com.github.ringmydevice.ui.maps

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.telephony.CellIdentity
import android.telephony.CellIdentityCdma
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfo
import android.telephony.TelephonyManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ringmydevice.viewmodel.SettingsViewModel
import com.github.ringmydevice.permissions.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

@Composable
fun MapScreen(modifier: Modifier = Modifier, settingsViewModel: SettingsViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val token by settingsViewModel.openCellIdToken.collectAsState()
    val scope = rememberCoroutineScope()

    var deviceLocation by remember { mutableStateOf<Location?>(null) }
    var cellLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var cellStatus by remember { mutableStateOf<String?>(null) }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(5.0)
        }
    }

    fun updateMarkers() {
        mapView.overlays.clear()
        deviceLocation?.let { loc ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(loc.latitude, loc.longitude)
                title = "This device"
                icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
            }
            mapView.overlays.add(marker)
            mapView.controller.setCenter(marker.position)
            mapView.controller.setZoom(14.0)
        }
        cellLocation?.let { point ->
            val marker = Marker(mapView).apply {
                position = point
                title = "Cell estimate (OpenCelliD)"
                icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    LaunchedEffect(deviceLocation, cellLocation) { updateMarkers() }

    LaunchedEffect(Unit) {
        val loc = loadLastLocation(context)
        deviceLocation = loc
    }

    Box(modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = {
                val loc = deviceLocation ?: loadLastLocation(context)
                deviceLocation = loc
                loc?.let {
                    mapView.controller.setCenter(GeoPoint(it.latitude, it.longitude))
                    mapView.controller.setZoom(15.0)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Locate")
        }
    }
}

private fun loadLastLocation(context: Context): Location? {
    val hasPermission = Permissions.has(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
        Permissions.has(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (!hasPermission) return null
    val lm = context.getSystemService(LocationManager::class.java) ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    val locations = providers.mapNotNull { provider ->
        runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
    }
    return locations.maxByOrNull { it.time }
}

private suspend fun fetchOpenCellId(context: Context, token: String): Pair<GeoPoint?, String>? =
    withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext null
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return@withContext null
        val hasLocation = Permissions.has(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            Permissions.has(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!hasLocation) return@withContext null
        val cell = telephony.allCellInfo?.firstOrNull { it.isRegistered } ?: return@withContext null
        val params = cell.buildParams() ?: return@withContext Pair(null, "Unable to read cell parameters")
        val url = params.toUrl(token)
        val response = runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        }.getOrElse { err ->
            return@withContext Pair(null, "OpenCelliD: $url\nError: ${err.message ?: "Unknown error"}")
        }
        return@withContext if (response.contains("\"lat\"") && response.contains("\"lon\"")) {
            val lat = "\"lat\":".let { key -> response.substringAfter(key).substringBefore(",").toDoubleOrNull() }
            val lon = "\"lon\":".let { key -> response.substringAfter(key).substringBefore(",").toDoubleOrNull() }
            if (lat != null && lon != null) {
                Pair(GeoPoint(lat, lon), "OpenCelliD: $url\nLat=$lat Lon=$lon")
            } else {
                Pair(null, "OpenCelliD: $url\nError: Unable to parse coordinates")
            }
        } else {
            Pair(null, "OpenCelliD: $url\n${response.take(200)}")
        }
    }

private data class CellParams(
    val mcc: Int,
    val mnc: Int,
    val lac: Int,
    val cellId: Int,
    val radio: String
) {
    fun toUrl(token: String): String =
        "https://opencellid.org/cell/get?key=$token&mcc=$mcc&mnc=$mnc&lac=$lac&cellid=$cellId&radio=${radio}&format=json"
}

@Suppress("DEPRECATION")
private fun CellInfo.buildParams(): CellParams? {
    val id: CellIdentity = when (this) {
        is android.telephony.CellInfoLte -> cellIdentity
        is android.telephony.CellInfoGsm -> cellIdentity
        is android.telephony.CellInfoWcdma -> cellIdentity
        is android.telephony.CellInfoCdma -> cellIdentity
        is android.telephony.CellInfoNr -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cellIdentity else return null
        else -> return null
    }
    return when (id) {
        is CellIdentityLte -> CellParams(
            mcc = id.mcc ?: return null,
            mnc = id.mnc ?: return null,
            lac = id.tac,
            cellId = id.ci,
            radio = "LTE"
        )
        is CellIdentityGsm -> CellParams(
            mcc = id.mcc ?: return null,
            mnc = id.mnc ?: return null,
            lac = id.lac,
            cellId = id.cid,
            radio = "GSM"
        )
        is CellIdentityWcdma -> CellParams(
            mcc = id.mcc ?: return null,
            mnc = id.mnc ?: return null,
            lac = id.lac,
            cellId = id.cid,
            radio = "UMTS"
        )
        is CellIdentityCdma -> CellParams(
            mcc = 0,
            mnc = 0,
            lac = id.networkId,
            cellId = id.basestationId,
            radio = "CDMA"
        )
        is CellIdentityNr -> {
            val mcc = id.mccString?.toIntOrNull() ?: return null
            val mnc = id.mncString?.toIntOrNull() ?: return null
            val nci = id.nci.toInt()
            val tac = id.tac ?: return null
            CellParams(mcc = mcc, mnc = mnc, lac = tac, cellId = nci, radio = "NR")
        }
        else -> null
    }
}
