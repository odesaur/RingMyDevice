package com.github.ringmydevice.ui.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.ringmydevice.data.repo.RmdServerRepository
import com.github.ringmydevice.util.imageToByteArray
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.Date
import java.util.Locale

class CameraCaptureActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var cameraExtra: Int = CAMERA_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasCameraPermission()) {
            Toast.makeText(this, "Camera permission missing; cannot take photo.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        // Plain layout; no preview needed for headless capture.
        setContentView(FrameLayout(this))

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onResume() {
        super.onResume()
        if (!this::cameraExecutor.isInitialized) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
        cameraExtra = intent.extras?.getInt(EXTRA_CAMERA) ?: CAMERA_BACK
        lifecycleScope.launch { takePhoto() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private suspend fun takePhoto() {
        val cameraProvider = ProcessCameraProvider.getInstance(this).await()
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .setTargetRotation(Surface.ROTATION_0)
            .setResolutionSelector(
                ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(
                        Size(720, 1280),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                ).build()
            )
            .build()

        val cameraSelector =
            if (cameraExtra == CAMERA_FRONT) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider.unbindAll()
        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Toast.makeText(this, "Camera bind failed: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val img = image.image
                    if (img == null) {
                        Toast.makeText(applicationContext, "Captured image was null", Toast.LENGTH_LONG).show()
                        finish()
                        return
                    }
                    val imgBytes = imageToByteArray(img)
                    image.close()
                    uploadPhotoAndFinish(imgBytes)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Toast.makeText(applicationContext, "Failed to take picture: ${exception.imageCaptureError}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        )
    }

    private fun uploadPhotoAndFinish(imgBytes: ByteArray) {
        lifecycleScope.launch {
            saveToGallery(imgBytes)
            val success = RmdServerRepository.getInstance().uploadPicture(imgBytes)
            val message = if (success) "Photo uploaded to server." else "Failed to upload photo."
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun saveToGallery(imgBytes: ByteArray) {
        runCatching {
            val resolver = contentResolver
            val filename = "RMD_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RMD")
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return
            resolver.openOutputStream(uri)?.use { it.write(imgBytes) }
        }.onFailure {
            Toast.makeText(applicationContext, "Saved locally failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_CAMERA = "camera"
        const val CAMERA_BACK = 0
        const val CAMERA_FRONT = 1
    }
}
