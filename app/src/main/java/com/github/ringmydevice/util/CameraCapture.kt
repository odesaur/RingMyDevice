package com.github.ringmydevice.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

object CameraCapture {
    suspend fun capture(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        facing: String
    ): Uri? {

        val cameraProvider = ProcessCameraProvider.getInstance(context).get()

        val selector = if (facing.equals("front", ignoreCase = true))
            CameraSelector.DEFAULT_FRONT_CAMERA
        else
            CameraSelector.DEFAULT_BACK_CAMERA

        val imageCapture = ImageCapture.Builder()
            .setTargetResolution(android.util.Size(1280, 720)) // MMS safe
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            cameraProvider.unbindAll()
        } catch (_: Exception) {}

        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        val uri = createMediaStoreUri(context) ?: return null

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            uri,
            ContentValues()
        ).build()

        return suspendCancellableCoroutine { cont ->
            val executor = ContextCompat.getMainExecutor(context)

            imageCapture.takePicture(
                outputOptions,
                executor,
                object : ImageCapture.OnImageSavedCallback {

                    override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                        if (cont.isActive) cont.resume(uri)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        exc.printStackTrace()
                        if (cont.isActive) cont.resume(null)
                    }
                }
            )
        }
    }


    private fun createMediaStoreUri(context: Context): Uri? {
        val name =
            "rmd_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"

        val cv = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RingMyDevice")
        }

        return context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            cv
        )
    }
}