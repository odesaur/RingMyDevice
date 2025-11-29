package com.github.ringmydevice.util

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Convert a CameraX Image to JPEG bytes.
 * Supports JPEG directly and YUV_420_888 via NV21 -> JPEG conversion.
 */
fun imageToByteArray(image: Image): ByteArray {
    return when (image.format) {
        ImageFormat.JPEG -> jpegImageToByteArray(image)
        ImageFormat.YUV_420_888 -> yuv420ToJpeg(image, 90) ?: ByteArray(0)
        else -> {
            Log.e("ImageUtils", "Unknown image format: ${image.format}")
            ByteArray(0)
        }
    }
}

private fun jpegImageToByteArray(image: Image): ByteArray {
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}

// Based on: https://blog.minhazav.dev/how-to-convert-yuv-420-sp-android.media.Image-to-Bitmap-or-jpeg/
private fun yuv420ToJpeg(image: Image, quality: Int): ByteArray? {
    require(image.format == ImageFormat.YUV_420_888) { "Invalid image format" }
    val nv21 = toNv21(image)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
    return ByteArrayOutputStream().use { out ->
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), quality, out)
        out.toByteArray()
    }
}

private fun toNv21(image: Image): ByteArray {
    require(image.format == ImageFormat.YUV_420_888) { "Invalid image format" }
    val width = image.width
    val height = image.height

    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]

    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer

    val numPixels = width * height
    val nv21 = ByteArray(numPixels + 2 * (numPixels / 4))

    // Copy Y
    var outputPos = 0
    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride
    for (row in 0 until height) {
        var inputPos = row * yRowStride
        for (col in 0 until width) {
            nv21[outputPos++] = yBuffer[inputPos]
            inputPos += yPixelStride
        }
    }

    // Copy UV interleaved (NV21 expects V then U)
    val uvRowStride = uPlane.rowStride
    val uvPixelStride = uPlane.pixelStride
    val halfHeight = height / 2
    val halfWidth = width / 2
    for (row in 0 until halfHeight) {
        var inputPos = row * uvRowStride
        for (col in 0 until halfWidth) {
            nv21[outputPos++] = vBuffer[inputPos]
            nv21[outputPos++] = uBuffer[inputPos]
            inputPos += uvPixelStride
        }
    }
    return nv21
}
