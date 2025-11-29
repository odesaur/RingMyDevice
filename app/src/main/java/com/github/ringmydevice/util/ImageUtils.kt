package com.github.ringmydevice.util

import android.media.Image
import java.nio.ByteBuffer

/**
 * Convert an Image from CameraX to a byte array.
 * Uses planes[0] which is sufficient for JPEG output produced by ImageCapture.
 */
fun imageToByteArray(image: Image): ByteArray {
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}
