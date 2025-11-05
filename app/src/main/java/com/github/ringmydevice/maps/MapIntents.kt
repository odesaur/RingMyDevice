package com.github.ringmydevice.maps

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Open OpenStreetMap in the browser centered on [lat],[lon] at [zoom]. */
fun openInOpenStreetMap(context: Context, lat: Double, lon: Double, zoom: Int = 16) {
    val url = "https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=${zoom}/${lat}/${lon}"
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
