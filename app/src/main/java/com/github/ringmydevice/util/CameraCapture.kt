package com.github.ringmydevice.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object CameraCapture {

    private const val TAG = "RMD_CAMERA"

    suspend fun capture(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        facing: String
    ): Uri? = withContext(Dispatchers.Main) {
        try {

            val cameraProvider = ProcessCameraProvider.getInstance(context).get()

            val selector =
                if (facing == "front")
                    CameraSelector.DEFAULT_FRONT_CAMERA
                else
                    CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()

            val imageCapture = ImageCapture.Builder()
                .setJpegQuality(80)
                .build()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                imageCapture
            )

            withContext(Dispatchers.IO) {
                takePictureToGallery(context, imageCapture)
            }
        } catch (e: Exception) {
            Log.e(TAG, "CameraCapture failed", e)
            null
        }
    }

    private suspend fun takePictureToGallery(
        context: Context,
        imageCapture: ImageCapture
    ): Uri? = suspendCancellableCoroutine { cont ->

        try {
            val fileName = "rmd_${System.currentTimeMillis()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/RingMyDevice"
                )
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()


            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {

                    override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                        val uri = result.savedUri
                        cont.resume(uri)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "takePicture failed", exception)
                        cont.resume(null)
                    }
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "takePictureToGallery crashed", e)
            cont.resume(null)
        }
    }
}